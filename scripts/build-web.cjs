const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
process.chdir(path.resolve(__dirname, '..'));
fs.mkdirSync('public', {recursive:true});
for (const name of ['index.html','app.js','manifest.json','config.js','features.js','trading-core.js']) fs.copyFileSync(name, 'public/'+name);
if (process.env.SUPABASE_URL || process.env.SUPABASE_ANON_KEY) {
  if (!process.env.SUPABASE_URL || !process.env.SUPABASE_ANON_KEY) throw new Error('Set both Supabase public variables');
  const ctx = {window:{}};
  vm.runInNewContext(fs.readFileSync('config.js','utf8'),ctx);
  const cfg = {...ctx.window.APP_CONFIG, supabaseUrl:process.env.SUPABASE_URL.trim(),supabaseAnonKey:process.env.SUPABASE_ANON_KEY.trim()};
  const url = new URL(cfg.supabaseUrl);
  if (url.protocol !== 'https:' || !url.hostname.endsWith('.supabase.co')) throw new Error('Invalid Supabase URL');
  if (!cfg.supabaseAnonKey.startsWith('sb_publishable_')) {
    const payload = JSON.parse(Buffer.from(cfg.supabaseAnonKey.split('.')[1] || '', 'base64url').toString());
    if (payload.role !== 'anon') throw new Error('Use an anon or publishable key only');
    if (payload.ref && url.hostname.split('.')[0] !== payload.ref) throw new Error('URL/key project mismatch');
  }
  fs.writeFileSync('public/config.js','window.APP_CONFIG = '+JSON.stringify(cfg,null,2)+';\n');
}
console.log('Public assets synchronized');

fs.cpSync("vendor", "public/vendor", {recursive:true});
