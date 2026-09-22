(function(root){
 'use strict';
 function active(profile,now=Date.now()) {return !!profile && profile.status==='approved' && (profile.role==='admin'||Date.parse(profile.access_until)>now);}
 function money(value){return Number.isFinite(value)?new Intl.NumberFormat('en-IN',{style:'currency',currency:'INR'}).format(value):'Unavailable';}
 function positions(rows){
  const map=new Map();
  for(const r of [...rows].sort((a,b)=>a.trade_date.localeCompare(b.trade_date)||a.created_at.localeCompare(b.created_at)||a.id.localeCompare(b.id))){
   const key=r.exchange+':'+r.symbol;let p=map.get(key);if(!p){p={symbol:r.symbol,exchange:r.exchange,quantity:0,cost:0,realized:0,buys:0,sells:0,fees:0,unmatched:false};map.set(key,p);}
   const q=Number(r.quantity),price=Number(r.price),fee=Number(r.fees);p.fees+=fee;
   if(r.side==='BUY'){p.quantity+=q;p.cost+=q*price+fee;p.buys+=q*price;}
   else {p.sells+=q*price; if(q>p.quantity+1e-8){p.unmatched=true;p.quantity-=q;p.cost=0;p.realized=null;}else{const basis=p.quantity>0?p.cost/p.quantity*q:0;p.quantity-=q;p.cost-=basis;if(p.realized!==null)p.realized+=q*price-fee-basis;}}
  }
  return [...map.values()].map(p=>({...p,average:p.quantity>0&&!p.unmatched?p.cost/p.quantity:null,cost:p.unmatched?null:p.cost,realized:p.unmatched?null:p.realized}));
 }
 function workbook(ExcelJS,rows){
  const wb=new ExcelJS.Workbook();wb.creator='Chart Analyzer Pro';wb.created=new Date();wb.calcProperties.fullCalcOnLoad=true;
  const summary=wb.addWorksheet('Summary');
  summary.addRow(['Trade history analysis snapshot (INR)']);
  summary.addRow(['Summary is a snapshot. Re-export after edits. Weighted-average cost includes buy fees; no live valuation/tax calculation.']);
  summary.addRow(['Unmatched sells produce unavailable cost/P&L. Same-day records use saved order.']);
  summary.addRow(['Exchange','Symbol','Net quantity','Cost basis INR','Average cost INR','Realized P&L INR','Buy gross INR','Sell gross INR','Fees INR','Review']);
  for(const p of positions(rows))summary.addRow([p.exchange,p.symbol,p.quantity,p.cost,p.average,p.realized,p.buys,p.sells,p.fees,p.unmatched?'Sell without sufficient recorded buys':'']);
  const sheet=wb.addWorksheet('Trades');
  sheet.addRow(['Trade ID','Date (IST)','Exchange','Symbol','Side','Quantity','Price INR','Fees INR','Gross INR','Net cash flow INR','Notes','Recorded at UTC']);
  for(const r of rows){const n=sheet.rowCount+1;const q=Number(r.quantity),p=Number(r.price),f=Number(r.fees);sheet.addRow([r.id,r.trade_date,r.exchange,r.symbol,r.side,q,p,f,{formula:'F'+n+'*G'+n,result:q*p},{formula:'IF(E'+n+'="BUY",-I'+n+'-H'+n+',I'+n+'-H'+n+')',result:(r.side==='BUY'?-q*p:q*p)-f},String(r.notes||''),r.created_at]);}
  sheet.views=[{state:'frozen',ySplit:1}];sheet.autoFilter={from:'A1',to:'L'+Math.max(1,sheet.rowCount)};
  summary.views=[{state:'frozen',ySplit:4}];
  for(const ws of [summary,sheet]){ws.columns.forEach(c=>c.width=22);const row=ws.getRow(ws===summary?4:1);row.font={bold:true,color:{argb:'FFFFFFFF'}};row.fill={type:'pattern',pattern:'solid',fgColor:{argb:'FF16324F'}};row.height=24;}
  sheet.getColumn(1).width=38;sheet.getColumn(11).width=45;
  for(const n of [7,8,9,10])sheet.getColumn(n).numFmt='#,##0.00;[Red](#,##0.00)';
  sheet.getColumn(6).numFmt='0.######';
  for(const n of [4,5,6,7,8,9])summary.getColumn(n).numFmt='#,##0.00;[Red](#,##0.00)';
  return wb;
 }
 root.TradingCore={active,money,positions,workbook};
 if(typeof module!=='undefined')module.exports=root.TradingCore;
})(typeof window!=='undefined'?window:globalThis);
