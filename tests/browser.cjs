const fs=require('node:fs'),path=require('node:path'),http=require('node:http'),assert=require('node:assert/strict');
const {chromium}=require('playwright');
const root=path.resolve('public');
const server=http.createServer((req,res)=>{const file=path.join(root,decodeURIComponent(req.url.split('?')[0]==='/'?'/index.html':req.url.split('?')[0]));if(!file.startsWith(root+path.sep)){res.writeHead(403);return res.end();}fs.readFile(file,(err,data)=>{if(err){res.writeHead(404);return res.end();}res.setHeader('Content-Type',file.endsWith('.js')?'text/javascript':file.endsWith('.html')?'text/html':'application/octet-stream');res.end(data);});});
(async()=>{await new Promise(r=>server.listen(0,'127.0.0.1',r));let browser;
try{
 browser=await chromium.launch({channel:'msedge',headless:true});const page=await browser.newPage({viewport:{width:1440,height:1000}});const errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.route('https://cdnjs.cloudflare.com/**',r=>r.fulfill({body:'',contentType:'text/css'}));
 await page.route('**/npm/@supabase/supabase-js@2/**',async route=>route.fulfill({contentType:'text/javascript',body:
  'window.supabase={createClient:()=>window.__client};'}));
 await page.addInitScript(()=>{
  const user={id:'11111111-1111-4111-8111-111111111111',full_name:'Test User',mobile_number:'1234567890',email:'test@example.invalid',status:'approved',role:'admin',access_until:new Date(Date.now()+86400000).toISOString()};
  window.__db={app_users:[user],access_plans:[{code:'trial_7',name:'Free trial',days:7,price_inr:0,enabled:true},{code:'paid_7',name:'7 days',days:7,price_inr:null,enabled:false},{code:'paid_30',name:'30 days',days:30,price_inr:null,enabled:false}],access_events:[],access_requests:[],trade_history:[]};
  window.__calls=[];
  class Query{
   constructor(table){this.table=table;this.filters=[];this.action='read';}
   select(){return this;}order(){return this;}limit(){return this;}range(){return this;}eq(key,value){this.filters.push([key,value]);return this;}insert(data){this.action='insert';this.data=data;return this;}update(data){this.action='update';this.data=data;return this;}delete(){this.action='delete';return this;}
   async execute(){let rows=window.__db[this.table].filter(r=>this.filters.every(([k,v])=>r[k]===v));if(this.action==='insert'){rows=[{...this.data,id:'trade-1',created_at:new Date().toISOString()}];window.__db[this.table].push(...rows);}if(this.action==='update')rows.forEach(r=>Object.assign(r,this.data));if(this.action==='delete')window.__db[this.table]=window.__db[this.table].filter(r=>!rows.includes(r));return {data:rows,error:null};}
   then(a,b){return this.execute().then(a,b);}async single(){const r=await this.execute();return {...r,data:r.data[0]};}
  }
  window.__client={auth:{getSession:async()=>({data:{session:{access_token:'test'}}}),getUser:async()=>({data:{user}}),signOut:async()=>({}),onAuthStateChange:()=>({data:{subscription:{unsubscribe(){}}}}),resetPasswordForEmail:async(email,options)=>{window.__reset={email,options};return {error:null};},updateUser:async({password})=>{window.__updatedPassword=password;return {error:null};}},from:t=>new Query(t),rpc:async(name,args)=>{window.__calls.push({name,args});return {data:name==='has_active_access'?true:null,error:null};}};
 });
 let open=true,requests=0;
 await page.route('**/functions/v1/upstox-market-data',async route=>{requests++;const body=route.request().postDataJSON();const market={status:open?'NORMAL_OPEN':'NORMAL_CLOSE',isOpen:open};let data={market,paused:true};if(!body.refresh||open){const candles=Array.from({length:60},(_,i)=>({time:new Date(Date.UTC(2026,6,1+i)).toISOString().slice(0,10),open:95+i/10,high:100+i/10,low:94+i/10,close:96+i/10,volume:1000+i}));data={market,candles,quote:{price:100,change:2,previousClose:98,lastTradeAt:new Date().toISOString()},source:'upstox'};}await route.fulfill({contentType:'application/json',body:JSON.stringify(data)});});
 await page.goto('http://127.0.0.1:'+server.address().port);await page.waitForFunction(()=>document.getElementById('selectedStockPrice').textContent.includes('100.00'));
 assert.deepEqual(errors,[]);
 await page.getByText('Forgot password?',{exact:true}).click();await page.fill('#resetEmail','reset@example.invalid');await page.getByRole('button',{name:'Send password reset link',exact:true}).click();assert.equal(await page.evaluate(()=>window.__reset.email),'reset@example.invalid');assert.match(await page.textContent('#authAlert'),/If this email is registered/);
 await page.evaluate(()=>showPasswordRecovery());await page.fill('#recoveryPassword','new-password-123');await page.fill('#recoveryPasswordConfirm','new-password-123');await page.getByRole('button',{name:'Save new password',exact:true}).click();assert.equal(await page.evaluate(()=>window.__updatedPassword),'new-password-123');assert.match(await page.textContent('#authAlert'),/Password updated/);
 await page.fill('#tradeSymbol','TCS');await page.fill('#tradeDate','2026-09-21');await page.fill('#tradeQuantity','10');await page.fill('#tradePrice','100');await page.fill('#tradeFees','10');await page.fill('#tradeNotes','=HYPERLINK("https://example.invalid")');await page.click('#tradeSave');await page.waitForFunction(()=>document.getElementById('tradeTableBody').textContent.includes('TCS'));
 const download=page.waitForEvent('download');await page.getByRole('button',{name:'Download Excel (.xlsx)',exact:true}).click();const file=await download;assert.ok(file.suggestedFilename().endsWith('.xlsx'));
 open=false;await page.evaluate(()=>refreshMarket(false));const closed=await page.evaluate(()=>JSON.stringify(currentCandles));await page.evaluate(()=>refreshMarket(false));assert.equal(await page.evaluate(()=>JSON.stringify(currentCandles)),closed);assert.match(await page.textContent('#marketState'),/NORMAL_CLOSE/);
 await page.fill('#price_paid_7','99');await page.check('#enabled_paid_7');await page.locator('button[onclick*="savePlan"]').first().click();assert.ok(await page.evaluate(()=>window.__calls.some(c=>c.name==='admin_set_plan'&&c.args.p_price===99)));
 fs.mkdirSync('tests/artifacts',{recursive:true});await page.screenshot({path:'tests/artifacts/desktop.png',fullPage:true});
 await page.setViewportSize({width:390,height:844});await page.screenshot({path:'tests/artifacts/mobile.png',fullPage:false});assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth),'Mobile page has horizontal overflow');
 assert.deepEqual(errors,[]);console.log('Browser checks passed: authenticated chart, trade entry, Excel download, closed-market freeze, admin pricing, desktop/mobile render. Requests: '+requests);
}finally{if(browser)await browser.close();server.close();}})().catch(e=>{console.error(e);process.exitCode=1;});
