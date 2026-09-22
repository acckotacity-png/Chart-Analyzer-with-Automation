// All market data comes from Upstox; no generated prices or user-supplied tokens.
const cors={'Access-Control-Allow-Origin':'*','Access-Control-Allow-Headers':'authorization,x-client-info,apikey,content-type','Access-Control-Allow-Methods':'POST,OPTIONS'};
function reply(body:unknown,status=200){return new Response(JSON.stringify(body),{status,headers:{...cors,'Content-Type':'application/json','Cache-Control':'no-store'}});}
let instruments: any[]=[];
let instrumentsAt=0;
async function resolveInstrument(symbol:string){
 if(Date.now()-instrumentsAt>12*3600000 || !instruments.length){
  const r=await fetch('https://assets.upstox.com/market-quote/instruments/exchange/NSE.json.gz',{signal:AbortSignal.timeout(20000)});
  if(!r.ok) throw new Error('Instrument directory unavailable');
  const bytes=new Uint8Array(await r.arrayBuffer());
  const raw=bytes[0]===31 && bytes[1]===139 ? await new Response(new Blob([bytes]).stream().pipeThrough(new DecompressionStream('gzip'))).text() : new TextDecoder().decode(bytes);
  const rows=JSON.parse(raw);
  if(!Array.isArray(rows)) throw new Error('Invalid instrument directory');
  instruments=rows.filter((x:any)=>x.segment==='NSE_EQ' && x.instrument_type==='EQ'); instrumentsAt=Date.now();
 }
 const item=instruments.find((x:any)=>x.trading_symbol===symbol);
 if(!item?.instrument_key) throw new Error('NSE equity symbol not found; use the current exchange symbol');
 return item;
}
function istDate(ms=Date.now()){return new Date(ms+19800000).toISOString().slice(0,10);}
function normalize(rows:any[],daily:boolean){
 const unique=new Map<string|number,any>();
 for(const c of rows){
  if(!Array.isArray(c)||c.length<6) throw new Error('Invalid broker candle');
  const ms=Date.parse(c[0]); const [open,high,low,close,volume]=c.slice(1,6).map(Number);
  if(![ms,open,high,low,close,volume].every(Number.isFinite)||open<=0||close<=0||low<=0||high<Math.max(open,close)||low>Math.min(open,close)||volume<0) throw new Error('Invalid broker candle');
  const time=daily?String(c[0]).slice(0,10):Math.floor(ms/1000);
  unique.set(time,{time,open,high,low,close,volume});
 }
 return [...unique.values()].sort((a,b)=>a.time<b.time?-1:a.time>b.time?1:0);
}
Deno.serve(async(req:Request)=>{
 if(req.method==='OPTIONS')return new Response(null,{status:204,headers:cors});
 if(req.method!=='POST')return reply({message:'Use POST'},405);
 try{
  const authorization=req.headers.get('authorization');
  if(!authorization?.startsWith('Bearer '))return reply({message:'Sign in required'},401);
  const base=Deno.env.get('SUPABASE_URL');const anon=Deno.env.get('SUPABASE_ANON_KEY');
  if(!base||!anon)return reply({message:'Supabase configuration missing'},503);
  const headers={apikey:anon,Authorization:authorization};
  const user=await fetch(base+'/auth/v1/user',{headers,signal:AbortSignal.timeout(10000)});
  if(!user.ok)return reply({message:'Invalid session'},401);
  const access=await fetch(base+'/rest/v1/rpc/has_active_access',{method:'POST',headers:{...headers,'Content-Type':'application/json'},body:'{}',signal:AbortSignal.timeout(10000)});
  if(!access.ok || await access.json()!==true)return reply({message:'Admin approval and an unexpired plan are required'},403);
  let body;try{body=await req.json();}catch{return reply({message:'Invalid JSON'},400);}
  if(!body||typeof body!=='object'||Array.isArray(body))return reply({message:'Invalid request'},400);
  const action=body.action||'get_snapshot';
  if(!['ping','get_snapshot','get_candles','market_status'].includes(action))return reply({message:'Unsupported action'},400);
  const token=Deno.env.get('UPSTOX_ACCESS_TOKEN');
  if(action==='ping')return reply({status:'success',source:'upstox',configured:!!token});
  if(!token)return reply({message:'Admin must configure UPSTOX_ACCESS_TOKEN in Supabase secrets'},503);
  async function broker(path:string){
   const res=await fetch('https://api.upstox.com'+path,{headers:{Authorization:'Bearer '+token,Accept:'application/json'},signal:AbortSignal.timeout(15000)});
   if(!res.ok)throw new Error('Broker request failed ('+res.status+'); check Upstox token and data entitlement');
   const data=await res.json();if(data.status!=='success')throw new Error('Broker returned unsuccessful data');return data.data;
  }
  let market:any={status:'UNKNOWN',isOpen:false,checkedAt:new Date().toISOString()};
  try{
   const m=await broker('/v2/market/status/NSE');
   const cas=m.cas_eligible_status?.status;
   market={...market,status:m.status||'UNKNOWN',isOpen:m.status==='NORMAL_OPEN' && (!cas||cas==='NORMAL_OPEN'),statusChangedAt:m.last_updated,auctionStatus:cas||null};
  }catch{market.message='Market status unavailable; automatic updates paused';}
  if(action==='market_status'||(body.refresh===true&&!market.isOpen))return reply({status:'success',market,paused:true});
  const symbol=typeof body.symbol==='string'?body.symbol.trim().toUpperCase():'';
  if(!/^[A-Z0-9&._-]{1,30}$/.test(symbol))return reply({message:'Invalid symbol'},400);
  const frames:Record<string,[string,number,number]>={'1m':['minutes',1,28],'5m':['minutes',5,28],'15m':['minutes',15,28],'1h':['hours',1,90],'1D':['days',1,180]};
  const timeframe=body.timeframe||'1D';
  if(!Object.hasOwn(frames,timeframe))return reply({message:'Unsupported timeframe'},400);
  const instrument=await resolveInstrument(symbol);const key=encodeURIComponent(instrument.instrument_key);
  const [unit,interval,days]=frames[timeframe];
  const historyTo=istDate(Date.now()-86400000),from=istDate(Date.now()-days*86400000);
  const [history,intraday,quotes]=await Promise.all([
   broker('/v3/historical-candle/'+key+'/'+unit+'/'+interval+'/'+historyTo+'/'+from),
   broker('/v3/historical-candle/intraday/'+key+'/'+(timeframe==='1D'?'minutes/1':unit+'/'+interval)),
   broker('/v2/market-quote/quotes?instrument_key='+key)
  ]);
  if(!Array.isArray(history?.candles)||!Array.isArray(intraday?.candles))throw new Error('Invalid candle response');
  let today=normalize(intraday.candles,false);
  let candles=normalize(history.candles,timeframe==='1D');
  if(timeframe==='1D'){
   const groups=new Map<string,any>();
   for(const c of today){const date=istDate(c.time*1000);const d=groups.get(date);if(d){d.high=Math.max(d.high,c.high);d.low=Math.min(d.low,c.low);d.close=c.close;d.volume+=c.volume;}else groups.set(date,{...c,time:date});}
   today=[...groups.values()];
  }
  const merged=new Map(candles.map(c=>[c.time,c]));for(const c of today)merged.set(c.time,c);
  candles=[...merged.values()].sort((a,b)=>a.time<b.time?-1:a.time>b.time?1:0).slice(-500);
  if(!candles.length)throw new Error('No market candles available');
  const quoteRows=Object.values(quotes||{}) as any[];
  // Upstox keys quote objects by display symbol (for example NSE_EQ:TCS), while the request uses an ISIN instrument key. A single-instrument request has one safe quote row.
  const q:any=quoteRows.find((row:any)=>row.instrument_token===instrument.instrument_key||row.instrument_key===instrument.instrument_key)||quoteRows[0];
  const price=Number(q?.last_price);const ltt=Number(q?.last_trade_time);const change=Number(q?.net_change);
  if(!q || !Number.isFinite(price)||price<=0||!Number.isFinite(ltt)||ltt<=0)throw new Error('Quote or exchange timestamp unavailable');
  return reply({status:'success',source:'upstox',symbol,instrumentKey:instrument.instrument_key,timeframe,market,candles,
   quote:{price,change:Number.isFinite(change)?change:null,previousClose:Number.isFinite(change)?price-change:null,lastTradeAt:new Date(ltt).toISOString(),fetchedAt:new Date().toISOString()},
   candleAsOf:candles.at(-1)?.time,intervalSeconds:15});
 }catch(error){return reply({message:error instanceof Error?error.message:'Market data unavailable'},502);}
});
