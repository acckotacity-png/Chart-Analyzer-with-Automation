// Supabase Credentials
const SUPABASE_URL = "https://qhqbporwncgccpcgrurl.supabase.co";
const SUPABASE_ANON = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFocWJwb3J3bmNnY2NwY2dydXJsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODk5NjMxOTIsImV4cCI6MjEwNTUzOTE5Mn0.o0EgKYIv0JQSxJJftgIpZwFA49br7tgTNdSkSmgYwMs";

let currentUser = JSON.parse(localStorage.getItem("app_user") || "null");

window.addEventListener("DOMContentLoaded", () => {
  if (currentUser) {
    checkActiveStatus();
  } else {
    showView("auth");
  }
});

function toggleAuth(showRegister) {
  document.getElementById("registerForm").classList.toggle("hidden", !showRegister);
  document.getElementById("loginForm").classList.toggle("hidden", showRegister);
  document.getElementById("authAlert").innerText = "";
}

async function handleRegister(e) {
  e.preventDefault();
  const name = document.getElementById("regName").value.trim();
  const mobile = document.getElementById("regMobile").value.trim();
  const email = document.getElementById("regEmail").value.trim().toLowerCase();
  const alertBox = document.getElementById("authAlert");

  alertBox.style.color = "var(--cyan)";
  alertBox.innerText = "Submitting registration request to Supabase...";

  try {
    // 1. Check if already exists
    const checkResp = await fetch(`${SUPABASE_URL}/rest/v1/app_users?email=eq.${email}&select=*`, {
      headers: { "apikey": SUPABASE_ANON, "Authorization": `Bearer ${SUPABASE_ANON}` }
    });
    const existing = await checkResp.json();
    if (existing && existing.length > 0) {
      currentUser = existing[0];
      localStorage.setItem("app_user", JSON.stringify(currentUser));
      checkActiveStatus();
      return;
    }

    // 2. Insert new pending record
    const insertResp = await fetch(`${SUPABASE_URL}/rest/v1/app_users`, {
      method: "POST",
      headers: {
        "apikey": SUPABASE_ANON,
        "Authorization": `Bearer ${SUPABASE_ANON}`,
        "Content-Type": "application/json",
        "Prefer": "return=representation"
      },
      body: JSON.stringify({
        full_name: name,
        mobile_number: mobile,
        email: email,
        status: "pending",
        role: "user"
      })
    });

    const data = await insertResp.json();
    if (insertResp.ok && data && data.length > 0) {
      currentUser = data[0];
      localStorage.setItem("app_user", JSON.stringify(currentUser));
      checkActiveStatus();
    } else {
      alertBox.style.color = "var(--red)";
      alertBox.innerText = "Error registering user: " + (data.message || "Please check details");
    }
  } catch (err) {
    alertBox.style.color = "var(--red)";
    alertBox.innerText = "Network error: " + err.message;
  }
}

async function handleLogin(e) {
  e.preventDefault();
  const identifier = document.getElementById("loginIdentifier").value.trim().toLowerCase();
  const alertBox = document.getElementById("authAlert");

  alertBox.style.color = "var(--cyan)";
  alertBox.innerText = "Verifying with Supabase...";

  const query = identifier.includes("@") ? `email=eq.${identifier}` : `mobile_number=eq.${identifier}`;

  try {
    const res = await fetch(`${SUPABASE_URL}/rest/v1/app_users?${query}&select=*`, {
      headers: { "apikey": SUPABASE_ANON, "Authorization": `Bearer ${SUPABASE_ANON}` }
    });
    const users = await res.json();
    if (users && users.length > 0) {
      currentUser = users[0];
      localStorage.setItem("app_user", JSON.stringify(currentUser));
      checkActiveStatus();
    } else {
      alertBox.style.color = "var(--red)";
      alertBox.innerText = "User record not found. Please register first.";
    }
  } catch (err) {
    alertBox.style.color = "var(--red)";
    alertBox.innerText = "Login error: " + err.message;
  }
}

function adminQuickLogin() {
  document.getElementById("loginIdentifier").value = "arjunmalviya166@gmail.com";
  toggleAuth(false);
  handleLogin({ preventDefault: () => {} });
}

async function checkActiveStatus() {
  if (!currentUser) {
    showView("auth");
    return;
  }

  try {
    const res = await fetch(`${SUPABASE_URL}/rest/v1/app_users?id=eq.${currentUser.id}&select=*`, {
      headers: { "apikey": SUPABASE_ANON, "Authorization": `Bearer ${SUPABASE_ANON}` }
    });
    const users = await res.json();
    if (users && users.length > 0) {
      currentUser = users[0];
      localStorage.setItem("app_user", JSON.stringify(currentUser));
    }
  } catch (e) {
    console.warn("Could not refresh live status", e);
  }

  // Update Header
  document.getElementById("userHeader").classList.remove("hidden");
  document.getElementById("headerUserName").innerText = currentUser.full_name;
  
  const statusBadge = document.getElementById("headerUserStatus");
  statusBadge.innerText = currentUser.status;
  statusBadge.className = `badge ${currentUser.status}`;

  if (currentUser.status === "approved") {
    showView("dashboard");
    if (currentUser.role === "admin") {
      document.getElementById("adminPanel").classList.remove("hidden");
      loadAdminUsers();
    } else {
      document.getElementById("adminPanel").classList.add("hidden");
    }
  } else {
    showView("pending");
    document.getElementById("pendingMessage").innerText = 
      `नमस्ते ${currentUser.full_name}, आपकी रिक्वेस्ट (Mobile: ${currentUser.mobile_number}, Email: ${currentUser.email}) एडमिन के पास पहुँच गई है। एडमिन द्वारा Rights (Approval) देने के बाद ही आप ऐप और चार्ट देख सकेंगे।`;
  }
}

async function loadAdminUsers() {
  const tbody = document.getElementById("userTableBody");
  tbody.innerHTML = `<tr><td colspan="4" style="text-align:center; color:var(--text-muted);">Loading registered users...</td></tr>`;

  try {
    const res = await fetch(`${SUPABASE_URL}/rest/v1/app_users?select=*&order=created_at.desc`, {
      headers: { "apikey": SUPABASE_ANON, "Authorization": `Bearer ${SUPABASE_ANON}` }
    });
    const users = await res.json();
    tbody.innerHTML = "";

    users.forEach(u => {
      const isApproved = u.status === "approved";
      const isRejected = u.status === "rejected";
      const isAdmin = u.role === "admin";

      const tr = document.createElement("tr");
      tr.innerHTML = `
        <td>
          <div style="font-weight:600;">${u.full_name} ${isAdmin ? '<span style="color:var(--cyan);">(Admin)</span>' : ''}</div>
          <div style="color:var(--text-muted); font-size:11px;">${u.email}</div>
        </td>
        <td>${u.mobile_number}</td>
        <td>
          <span class="badge ${u.status}">${u.status}</span>
        </td>
        <td>
          <div class="action-btn-group">
            ${!isApproved ? `<button class="btn-sm btn-green" onclick="updateUserStatus('${u.id}', 'approved')"><i class="fa-solid fa-check"></i> Approve</button>` : ''}
            ${!isRejected && !isAdmin ? `<button class="btn-sm btn-red" onclick="updateUserStatus('${u.id}', 'rejected')"><i class="fa-solid fa-xmark"></i> Reject</button>` : ''}
          </div>
        </td>
      `;
      tbody.appendChild(tr);
    });
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="4" style="color:var(--red);">Failed to load users: ${err.message}</td></tr>`;
  }
}

async function updateUserStatus(userId, status) {
  try {
    await fetch(`${SUPABASE_URL}/rest/v1/app_users?id=eq.${userId}`, {
      method: "PATCH",
      headers: {
        "apikey": SUPABASE_ANON,
        "Authorization": `Bearer ${SUPABASE_ANON}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        status: status,
        approved_by: currentUser.email,
        approved_at: new Date().toISOString()
      })
    });
    loadAdminUsers();
  } catch (e) {
    alert("Error updating status: " + e.message);
  }
}

function logout() {
  localStorage.removeItem("app_user");
  currentUser = null;
  document.getElementById("userHeader").classList.add("hidden");
  showView("auth");
}

function showView(view) {
  document.getElementById("authGate").classList.toggle("hidden", view !== "auth");
  document.getElementById("pendingGate").classList.toggle("hidden", view !== "pending");
  document.getElementById("appDashboard").classList.toggle("hidden", view !== "dashboard");
}
