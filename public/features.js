// Access plans, private trading journal, and broker-only refresh loop.
let marketTimer=null, marketGeneration=0, marketOpen=false, marketBusy=false;
let lastSnapshot=null, tradeRows=[], tradeOwner=null, accessPlans=[];
let accessRequestGeneration=0;
const el=id=>document.getElementById(id);
const safe=escapeHtml;
const money=TradingCore.money;
function hasActiveAccess(){return TradingCore.active(currentUser);}
function report(id,text){const node=el(id);if(node)node.textContent=text;}
async function rpc(name,args={}){const {data,error}=await authClient.rpc(name,args);if(error)throw error;return data;}
async function loadAccessPanel(){
 if(!currentUser)return;
 el('accessPanel').classList.remove('hidden');
 report('accessDetails',currentUser.role==='admin'?'Administrator access':('Status: '+currentUser.status+' | Plan: '+(currentUser.plan_code||'None')+' | Expires: '+(currentUser.access_until?new Date(currentUser.access_until).toLocaleString('en-IN',{timeZone:'Asia/Kolkata'})+' IST':'Not activated')));
 try{
  const {data,error}=await authClient.from('access_plans').select('*').order('days');if(error)throw error;accessPlans=data;
  el('planChoices').innerHTML=data.map(p=>'<button class="btn btn-secondary" '+(!p.enabled||(p.code==='trial_7'&&currentUser.trial_used)?'disabled':'')+' onclick="requestPlan(&quot;'+p.code+'&quot;)">'+safe(p.name)+' · '+(p.price_inr===null?'Price not set':money(Number(p.price_inr)))+'</button>').join(' ');
  const {data:events,error:e}=await authClient.from('access_events').select('*').eq('user_id',currentUser.id).order('created_at',{ascending:false}).limit(20);if(e)throw e;
  report('accessEventHistory',(events||[]).map(x=>new Date(x.created_at).toLocaleDateString('en-IN')+' — '+x.action+' '+(x.plan_code||'')+(x.ends_at?' until '+new Date(x.ends_at).toLocaleDateString('en-IN'):'')).join(' | ')||'No access activations yet.');
  if(currentUser.role==='admin')renderPlanSettings();
 }catch(e){report('accessMessage',e.message);}
}
async function requestPlan(code){try{await rpc('request_access',{p_plan:code});report('accessMessage','Request sent. Admin approval/payment confirmation is required; access does not start yet.');}catch(e){report('accessMessage',e.message);}}
function renderPlanSettings(){
 el('planSettings').innerHTML='<h3>Plan prices (INR)</h3><p>Trial: ₹0 / 7 days from admin approval. Paid access starts on approval; renewals extend remaining paid access. Record payment externally before activation.</p>'+accessPlans.filter(p=>p.code!=='trial_7').map(p=>'<div class="plan-setting"><label>'+safe(p.name)+' <input class="form-control" id="price_'+p.code+'" type="number" min="0" step="0.01" value="'+(p.price_inr??'')+'" placeholder="Set price"></label><label><input type="checkbox" id="enabled_'+p.code+'" '+(p.enabled?'checked':'')+'> Enabled</label><button class="btn btn-secondary" onclick="savePlan(&quot;'+p.code+'&quot;)">Save price</button></div>').join('');
}
async function savePlan(code){try{const raw=el('price_'+code).value;if(raw==='')throw new Error('Enter a price first');await rpc('admin_set_plan',{p_code:code,p_price:Number(raw),p_enabled:el('enabled_'+code).checked});report('planMessage','Plan saved. Previous payment records keep their original price.');await loadAccessPanel();}catch(e){report('planMessage',e.message);}}
function openAdminSection(section){
 if(currentUser?.role!=='admin')return;
 const rights=el('adminRightsPanel'),prices=el('adminPricesPanel');
 rights.classList.toggle('hidden',section!=='rights');prices.classList.toggle('hidden',section!=='prices');
 if(section==='rights')loadAdminUsers();else renderPlanSettings();
 (section==='rights'?rights:prices).scrollIntoView({behavior:'smooth',block:'start'});
}
function closeAdminSections(){el('adminRightsPanel')?.classList.add('hidden');el('adminPricesPanel')?.classList.add('hidden');}
async function loadAdminUsers(){
 if(currentUser?.role!=='admin')return;
 try{
  const {data:users,error}=await authClient.from('app_users').select('*').order('created_at');if(error)throw error;
  const {data:requests,error:e}=await authClient.from('access_requests').select('*').eq('status','pending');if(e)throw e;
  const byUser=new Map((requests||[]).map(r=>[r.user_id,r.plan_code]));
  el('userTableBody').innerHTML=users.map(u=>'<tr><td>'+safe(u.full_name)+'<br>'+safe(u.email)+'<br>Request: '+safe(byUser.get(u.id)||'None')+'</td><td>'+safe(u.mobile_number)+'</td><td>'+safe(u.status)+'<br>'+safe(u.plan_code||'No plan')+'<br>'+safe(u.access_until?new Date(u.access_until).toLocaleString('en-IN',{timeZone:'Asia/Kolkata'})+' IST':'No expiry')+'</td><td>'+(u.role==='admin'?'Admin':'<select class="form-control" id="grant_'+u.id+'"><option value="trial_7" '+(u.trial_used?'disabled':'')+'>Free trial · 7 days</option><option value="paid_7">Paid · 7 days</option><option value="paid_30">Paid · 30 days</option></select><input class="form-control" id="payment_'+u.id+'" placeholder="Paid plan: confirmed payment reference" maxlength="500"><button class="btn-sm btn-green" onclick="grantAccess(&quot;'+u.id+'&quot;)">Approve / extend</button> <button class="btn-sm btn-red" onclick="revokeAccess(&quot;'+u.id+'&quot;)">Revoke / reject</button>')+' <button class="btn-sm" onclick="viewUserHistory(&quot;'+u.id+'&quot;)">View trades</button></td></tr>').join('');
 }catch(e){report('adminMessage',e.message);}
}
async function grantAccess(id){try{await rpc('admin_grant_access',{p_user:id,p_plan:el('grant_'+id).value,p_reference:el('payment_'+id).value.trim()});report('adminMessage','Access activated and recorded.');await loadAdminUsers();}catch(e){report('adminMessage',e.message);}}
async function revokeAccess(id){try{await rpc('admin_revoke_access',{p_user:id});report('adminMessage','Access revoked. History remains saved.');await loadAdminUsers();}catch(e){report('adminMessage',e.message);}}
async function viewUserHistory(id){tradeOwner=id;cancelTradeEdit();await loadTradeHistory();el('journalPanel').scrollIntoView({behavior:'smooth'});}
async function ownHistory(){tradeOwner=currentUser.id;cancelTradeEdit();await loadTradeHistory();}
async function loadTradeHistory(){
 if(!hasActiveAccess())return;
 const owner=tradeOwner||currentUser.id;tradeOwner=owner;const userAtStart=currentUser.id;
 try{
  const rows=[];for(let offset=0;;offset+=1000){
   const {data,error}=await authClient.from('trade_history').select('*').eq('user_id',owner).order('trade_date').order('created_at').order('id').range(offset,offset+999);if(error)throw error;rows.push(...data);if(data.length<1000)break;
  }
  if(owner!==tradeOwner||currentUser?.id!==userAtStart||!hasActiveAccess())return;tradeRows=rows;
  el('tradeForm').classList.toggle('hidden',owner!==currentUser.id);
  report('journalOwner',owner===currentUser.id?'My purchase / sale records':'Admin view · User '+owner+' (read-only)');
  el('tradeTableBody').innerHTML=rows.slice().reverse().map(r=>'<tr><td>'+safe(r.trade_date)+'</td><td>'+safe(r.exchange+':'+r.symbol)+'</td><td>'+safe(r.side)+'</td><td>'+Number(r.quantity)+'</td><td>'+money(Number(r.price))+'</td><td>'+money(Number(r.fees))+'</td><td>'+safe(r.notes)+'</td><td>'+(owner===currentUser.id?'<button class="btn-sm" onclick="editTrade(&quot;'+r.id+'&quot;)">Edit</button> <button class="btn-sm btn-red" onclick="deleteTrade(&quot;'+r.id+'&quot;)">Delete</button>':'')+'</td></tr>').join('')||'<tr><td colspan="8">No saved trades. Add your actual purchase/sale details above.</td></tr>';
  el('holdingsBody').innerHTML=TradingCore.positions(rows).map(p=>'<tr><td>'+safe(p.exchange+':'+p.symbol)+'</td><td>'+Number(p.quantity.toFixed(6))+'</td><td>'+money(p.average)+'</td><td>'+money(p.realized)+'</td><td>'+(p.unmatched?'Missing buy history; P&L unavailable':'Weighted-average cost')+'</td></tr>').join('');
  report('journalMessage',rows.length+' saved records. INR; no live valuation or tax calculation.');
 }catch(e){if(owner!==tradeOwner||currentUser?.id!==userAtStart)return;tradeRows=[];el('tradeTableBody').innerHTML='';el('holdingsBody').innerHTML='';report('journalMessage',e.message);}
}
function cancelTradeEdit(){el('tradeForm').reset();el('tradeId').value='';el('tradeDate').value=new Date(Date.now()+19800000).toISOString().slice(0,10);report('tradeSaveLabel','Save trade');}
function editTrade(id){const r=tradeRows.find(r=>r.id===id);if(!r||r.user_id!==currentUser.id)return;el('tradeId').value=id;for(const [field,key] of [['tradeSymbol','symbol'],['tradeExchange','exchange'],['tradeSide','side'],['tradeDate','trade_date'],['tradeQuantity','quantity'],['tradePrice','price'],['tradeFees','fees'],['tradeNotes','notes']])el(field).value=r[key];report('tradeSaveLabel','Save changes');el('tradeForm').scrollIntoView({behavior:'smooth'});}
async function saveTrade(event){
 event.preventDefault();const button=el('tradeSave');if(button.disabled)return;button.disabled=true;
 try{
  if(!hasActiveAccess()||tradeOwner!==currentUser.id)throw new Error('Active access required');
  const row={symbol:el('tradeSymbol').value.trim().toUpperCase(),exchange:el('tradeExchange').value,side:el('tradeSide').value,trade_date:el('tradeDate').value,quantity:Number(el('tradeQuantity').value),price:Number(el('tradePrice').value),fees:Number(el('tradeFees').value||0),notes:el('tradeNotes').value.trim()};
  if(!/^[A-Z0-9&._-]{1,30}$/.test(row.symbol)||!Number.isFinite(row.quantity)||row.quantity<=0||!Number.isFinite(row.price)||row.price<=0||!Number.isFinite(row.fees)||row.fees<0)throw new Error('Enter valid symbol, quantity, price and fees');
  const id=el('tradeId').value;const query=id?authClient.from('trade_history').update(row).eq('id',id).eq('user_id',currentUser.id):authClient.from('trade_history').insert({...row,user_id:currentUser.id});
  const {data,error}=await query.select('id');if(error)throw error;if(!data?.length)throw new Error('Trade not saved; access may have expired');cancelTradeEdit();await loadTradeHistory();
 }catch(e){report('journalMessage',e.message);}finally{button.disabled=false;}
}
async function deleteTrade(id){if(!confirm('Delete this saved trade? An audit record will remain.'))return;try{const {data,error}=await authClient.from('trade_history').delete().eq('id',id).eq('user_id',currentUser.id).select('id');if(error)throw error;if(!data?.length)throw new Error('Delete denied');await loadTradeHistory();}catch(e){report('journalMessage',e.message);}}
async function exportTrades(){
 try{if(!await rpc('has_active_access'))throw new Error('Access expired or revoked');await loadTradeHistory();if(!tradeRows.length)throw new Error('No records to export');if(!window.ExcelJS)throw new Error('Excel library could not load; reload and retry');const wb=TradingCore.workbook(ExcelJS,tradeRows);const buffer=await wb.xlsx.writeBuffer();const url=URL.createObjectURL(new Blob([buffer],{type:'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'}));const a=document.createElement('a');a.href=url;a.download='trade-history-'+new Date().toISOString().slice(0,10)+'.xlsx';a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);}catch(e){report('journalMessage',e.message);}
}
async function fetchMarket(body){
 const res=await fetch(SUPABASE_URL+'/functions/v1/upstox-market-data',{method:'POST',headers:{'Content-Type':'application/json',apikey:SUPABASE_ANON,Authorization:'Bearer '+await sessionToken()},body:JSON.stringify(body),signal:AbortSignal.timeout(45000)});
 const data=await res.json();if(!res.ok){if(res.status===401||res.status===403){stopLivePriceStream();checkActiveStatus();}throw new Error(data.message||'Market data unavailable');}return data;
}
function marketLabel(data){const m=data.market;const q=data.quote;return (m.isOpen?'Market open · broker refresh every 15 seconds':'Market '+m.status+' · automatic chart updates paused')+(q?' | Last trade: '+new Date(q.lastTradeAt).toLocaleString('en-IN',{timeZone:'Asia/Kolkata'})+' IST':'');}
async function refreshMarket(manual=false){
 if(!hasActiveAccess()||marketBusy)return;
 marketBusy=true;const generation=marketGeneration;const symbol=selectedStock.symbol;const timeframe=currentTimeframe;
 try{
  let data=await fetchMarket({action:'get_snapshot',symbol,timeframe,refresh:!manual});
  if(data.paused && marketOpen && data.market.status!=='UNKNOWN') data=await fetchMarket({action:'get_snapshot',symbol,timeframe,refresh:false});
  if(generation!==marketGeneration||symbol!==selectedStock.symbol||timeframe!==currentTimeframe||!hasActiveAccess())return;
  marketOpen=data.market.isOpen;
  if(!data.paused){lastSnapshot=data;await loadStockChartAndAnalysis(selectedStock,data);}
  const label=marketLabel(data.paused?{...data,quote:lastSnapshot?.quote}:data);
  report('marketState',label);updateDataFeedBadge(label);
 }catch(e){if(generation===marketGeneration){marketOpen=false;report('marketState','Data unavailable / last values are stale: '+e.message);updateDataFeedBadge('Data unavailable — no simulated prices');}}
 finally{marketBusy=false;if(generation===marketGeneration&&hasActiveAccess()){clearTimeout(marketTimer);marketTimer=setTimeout(()=>refreshMarket(false),marketOpen?15000:60000);}}
}
function startLivePriceStream(){stopLivePriceStream();refreshMarket(true);}
function stopLivePriceStream(){marketGeneration++;clearTimeout(marketTimer);marketTimer=null;marketOpen=false;}
function onMarketSelection(){stopLivePriceStream();lastSnapshot=null;clearMarketDisplay();const generation=marketGeneration;function retry(){if(generation!==marketGeneration)return;if(marketBusy){marketTimer=setTimeout(retry,100);return;}refreshMarket(true);}retry();}
function clearMarketDisplay(){currentCandles=[];lastSnapshot=null;for(const series of [candleSeries,volumeSeries,emaSeries,smaSeries,vwapSeries,bbUpperSeries,bbLowerSeries])if(series)series.setData([]);if(candleSeries)for(const line of activeSrLines)candleSeries.removePriceLine(line);activeSrLines=[];document.querySelectorAll('[id^="ind"],[id^="aiBlock"]').forEach(node=>{if(!node.children.length)node.textContent='Awaiting market data';});report('signalBadge','Market data');report('selectedStockPrice','Unavailable');report('selectedStockChange','');}
function applyQuote(stock,data){stock.price=data.quote.price;stock.change=data.quote.change;stock.changePercent=data.quote.previousClose>0&&Number.isFinite(stock.change)?(100*stock.change/data.quote.previousClose).toFixed(2):null;stock.quoteTime=data.quote.lastTradeAt;report('selectedStockPrice',money(stock.price));report('selectedStockChange',Number.isFinite(stock.change)?money(stock.change)+(stock.changePercent!==null?' ('+stock.changePercent+'%)':''):'Change unavailable');renderSharesList();populateQuickStockDropdown();}
setInterval(async()=>{
 if(!currentUser||!authClient)return;const userId=currentUser.id;const wasActive=hasActiveAccess();
 try{const {data,error}=await authClient.from('app_users').select('*').eq('id',currentUser.id).single();if(error)throw error;if(currentUser?.id!==userId)return;currentUser=data;if(!hasActiveAccess()){stopLivePriceStream();clearMarketDisplay();tradeRows=[];el('tradeTableBody').innerHTML='';el('holdingsBody').innerHTML='';showView('pending');report('pendingMessage','Access expired, revoked or awaiting admin activation. Request a plan below.');}else if(!wasActive){await checkActiveStatus();}await loadAccessPanel();}catch{stopLivePriceStream();showView('auth');}
},60000);
