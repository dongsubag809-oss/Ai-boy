const http=require('node:http');
const https=require('node:https');
const crypto=require('node:crypto');
const PORT=Number(process.env.PORT||10000);
const KEY=process.env.OPENAI_API_KEY;
const ACCESS=process.env.AI_BOY_ACCESS_TOKEN;
const MODEL=process.env.OPENAI_MODEL||'gpt-4.1-mini';
const buckets=new Map();
function json(res,code,body){res.writeHead(code,{'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store','X-Content-Type-Options':'nosniff'});res.end(JSON.stringify(body));}
const server=http.createServer((req,res)=>{
 if(req.url==='/health'&&req.method==='GET')return json(res,200,{ok:true,configured:!!KEY&&!!ACCESS});
 if(req.url!=='/chat'||req.method!=='POST')return json(res,404,{error:'not found'});
 if(!KEY||!ACCESS)return json(res,503,{error:'Server configuration missing'});
 const token=(req.headers.authorization||'').replace(/^Bearer /,'');
 if(!token||token.length!==ACCESS.length||!crypto.timingSafeEqual(Buffer.from(token),Buffer.from(ACCESS)))return json(res,401,{error:'Unauthorized'});
 const addr=req.socket.remoteAddress||'unknown';const now=Date.now();const hits=(buckets.get(addr)||[]).filter(t=>now-t<60000);if(hits.length>=12)return json(res,429,{error:'Too many requests'});hits.push(now);buckets.set(addr,hits);if(buckets.size>1000)buckets.clear();
 let raw='';req.on('data',chunk=>{raw+=chunk;if(raw.length>8192)req.destroy();});req.on('end',()=>{
 let message;try{message=JSON.parse(raw).message;}catch{return json(res,400,{error:'Invalid JSON'});}
 if(typeof message!=='string'||!message.trim()||message.length>2000)return json(res,400,{error:'Invalid message'});
 const payload=JSON.stringify({model:MODEL,input:[{role:'system',content:'You are a friendly Korean speaking AI voice companion. Answer naturally and concisely in Korean.'},{role:'user',content:message.trim()}],max_output_tokens:400,store:false});
 const upstream=https.request('https://api.openai.com/v1/responses',{method:'POST',headers:{Authorization:'Bearer '+KEY,'Content-Type':'application/json','Content-Length':Buffer.byteLength(payload)},timeout:45000},up=>{
 let data='';up.on('data',chunk=>{data+=chunk;if(data.length>200000)up.destroy();});up.on('end',()=>{
 if(up.statusCode!==200)return json(res,502,{error:'OpenAI request failed',status:up.statusCode});
 try{const result=JSON.parse(data);const reply=(result.output||[]).flatMap(x=>x.content||[]).filter(x=>x.type==='output_text').map(x=>x.text).join(' ').trim();if(!reply)return json(res,502,{error:'Empty AI response'});json(res,200,{reply});}catch{json(res,502,{error:'Invalid AI response'});}
 });});up.on('timeout',()=>up.destroy(new Error('timeout')));up.on('error',()=>{if(!res.writableEnded)json(res,502,{error:'AI service unavailable'});});up.end(payload);
 });
});server.listen(PORT,'0.0.0.0',()=>console.log('AI Boy listening on '+PORT));
