package com.example.export

import com.example.BuildConfig
import com.example.data.model.ArmoryItem
import com.example.data.model.LinkedAccount
import com.example.data.model.MatchRecord
import com.example.data.model.OperatorProfile
import com.example.data.model.OriginalVaultCatalog
import com.example.data.model.RigAsset
import com.example.data.model.UniverseCatalog
import org.json.JSONArray
import org.json.JSONObject

data class PortableCollectionBundle(
  val callsign: String,
  val activeUniverseId: String,
  val activeMode: String,
  val activeRigId: String,
  val consolePreset: String,
  val totalEssence: Long,
  val highScore: Long,
  val highestWave: Long,
  val osWallpaperIndex: Long,
  val armory: List<ArmoryItem>,
  val rigs: List<RigAsset>,
  val matchLogs: List<MatchRecord>,
  val linkedAccounts: List<LinkedAccount>,
)

object PortablePackageGenerator {

  /**
   * Generates a real, self-contained HTML5/Canvas + W3C Gamepad API + iOS Safari touch-compatible
   * playable build of The Unknown Universes that runs offline in browsers across iOS, PS5/Xbox/PC
   * without needing console devkits or App Store distribution.
   */
  fun generatePlayableHtmlPackage(
    profile: OperatorProfile,
    armory: List<ArmoryItem>,
    rig: RigAsset?,
  ): String {
    val world = UniverseCatalog.worldById(profile.activeUniverseId)
    val mode = UniverseCatalog.modeById(profile.activeMode)
    val console = UniverseCatalog.consoleById(profile.consolePreset)
    val equipped = armory.filter { it.equipped }
    val accentHex = String.format("#%06X", 0xFFFFFF and world.accentHex.toInt())
    val secondaryHex = String.format("#%06X", 0xFFFFFF and world.secondaryHex.toInt())
    val rigName = rig?.name ?: "Vanguard Tactical Rig"
    val rigClip = rig?.activeClip ?: "idle"
    val equippedNames =
      if (equipped.isEmpty()) "Standard Loadout" else equipped.joinToString(" • ") { "${it.name} T${it.tier}" }
    val safeCallsign = escapeHtml(profile.callsign)
    val safeWorld = escapeHtml(world.title)
    val safeMode = escapeHtml(mode.label)
    val safeConsole = escapeHtml(console.label)
    val safeLoadout = escapeHtml(equippedNames)

    return """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no,viewport-fit=cover" />
<meta name="apple-mobile-web-app-capable" content="yes" />
<meta name="apple-mobile-web-app-status-bar-style" content="black-translucent" />
<title>The Unknown Universes — UnknownOS Portable</title>
<style>
  :root { --accent: $accentHex; --sec: $secondaryHex; --bg: #070a12; --panel: #0e1525; }
  * { box-sizing: border-box; user-select: none; -webkit-user-select: none; }
  body { margin: 0; background: var(--bg); color: #f0f4ff; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; overflow: hidden; display: flex; flex-direction: column; height: 100vh; }
  header { display: flex; justify-content: space-between; align-items: center; padding: 10px 16px; background: var(--panel); border-bottom: 1px solid #283655; font-size: 13px; }
  .badge { padding: 3px 8px; border-radius: 999px; background: rgba(0,229,255,0.14); border: 1px solid var(--accent); color: var(--accent); font-weight: 700; font-size: 11px; }
  #stageWrap { position: relative; flex: 1; display: flex; align-items: center; justify-content: center; }
  canvas { width: 100%; height: 100%; display: block; touch-action: none; }
  .hud { position: absolute; left: 12px; top: 12px; background: rgba(14,21,37,0.82); padding: 10px 12px; border-radius: 10px; border: 1px solid #283655; font-size: 12px; line-height: 1.45; pointer-events: none; }
  .controls { position: absolute; bottom: 16px; right: 16px; display: flex; gap: 10px; }
  button { background: var(--panel); color: #fff; border: 1.5px solid var(--accent); border-radius: 12px; padding: 12px 16px; font-weight: 700; font-size: 13px; cursor: pointer; }
  button:active { background: var(--accent); color: #001; }
</style>
</head>
<body>
<header>
  <div><strong>UNKNOWN<span style="color:var(--accent)">OS</span></strong> • $safeWorld</div>
  <div class="badge">$safeMode • $safeConsole</div>
</header>
<div id="stageWrap">
  <canvas id="arena"></canvas>
  <div class="hud">
    <div><strong>Operator:</strong> $safeCallsign</div>
    <div><strong>Rig:</strong> ${escapeHtml(rigName)} (${escapeHtml(rigClip)})</div>
    <div><strong>Loadout:</strong> $safeLoadout</div>
    <div id="stats">Score: 0 | Wave: 1 | Shield: 100 | Gamepad: Waiting</div>
    <div style="opacity:.75;margin-top:4px;">WASD/Arrows or Left Stick to Move • Space/Fire • E/Power</div>
  </div>
  <div class="controls">
    <button id="btnFire">FIRE</button>
    <button id="btnPower">POWER</button>
    <button id="btnDash">DASH</button>
  </div>
</div>
<script>
const canvas = document.getElementById('arena');
const ctx = canvas.getContext('2d');
const statsEl = document.getElementById('stats');
function resize(){ canvas.width = canvas.clientWidth; canvas.height = canvas.clientHeight; }
window.addEventListener('resize', resize); resize();

const state = {
  px: 0.5, py: 0.5, vx: 0, vy: 0, score: 0, wave: 1, shield: 100,
  powerCd: 0, bolts: [], enemies: [], keys: {}, frame: 0, padName: 'Keyboard/Touch'
};

function spawnWave(n){
  for(let i=0;i<n*4;i++){
    const a = Math.random()*Math.PI*2;
    state.enemies.push({
      x: 0.5 + Math.cos(a)*0.46, y: 0.5 + Math.sin(a)*0.46,
      hp: 1 + Math.floor(n/2), speed: 0.0012 + n*0.00018
    });
  }
}
spawnWave(1);

window.addEventListener('keydown', e => {
  state.keys[e.code] = true;
  if(e.code === 'Space') fireBolt();
  if(e.code === 'KeyE') triggerPower();
});
window.addEventListener('keyup', e => { state.keys[e.code] = false; });
canvas.addEventListener('pointerdown', e => {
  const r = canvas.getBoundingClientRect();
  const tx = (e.clientX - r.left)/r.width, ty = (e.clientY - r.top)/r.height;
  fireBoltTowards(tx - state.px, ty - state.py);
});
document.getElementById('btnFire').onclick = () => fireBolt();
document.getElementById('btnPower').onclick = () => triggerPower();
document.getElementById('btnDash').onclick = () => { state.vx *= 2.2; state.vy *= 2.2; };

function fireBolt(){ fireBoltTowards(1, 0); }
function fireBoltTowards(dx, dy){
  const len = Math.hypot(dx, dy) || 1;
  state.bolts.push({ x: state.px, y: state.py, vx: (dx/len)*0.014, vy: (dy/len)*0.014, life: 60 });
}
function triggerPower(){
  if(state.powerCd > 0) return;
  state.powerCd = 180;
  state.enemies = state.enemies.filter(e => Math.hypot(e.x - state.px, e.y - state.py) > 0.24);
  state.score += 120;
}

function pollGamepad(){
  const pads = navigator.getGamepads ? navigator.getGamepads() : [];
  for(const p of pads){
    if(!p) continue;
    state.padName = p.id.slice(0, 22);
    if(Math.abs(p.axes[0]) > 0.18) state.vx += p.axes[0]*0.004;
    if(Math.abs(p.axes[1]) > 0.18) state.vy += p.axes[1]*0.004;
    if(p.buttons[0]?.pressed && state.frame % 10 === 0) fireBolt();
    if(p.buttons[3]?.pressed) triggerPower();
    break;
  }
}

function tick(){
  state.frame++;
  if(state.powerCd > 0) state.powerCd--;
  const spd = 0.0042;
  if(state.keys['KeyW']||state.keys['ArrowUp']) state.vy -= spd;
  if(state.keys['KeyS']||state.keys['ArrowDown']) state.vy += spd;
  if(state.keys['KeyA']||state.keys['ArrowLeft']) state.vx -= spd;
  if(state.keys['KeyD']||state.keys['ArrowRight']) state.vx += spd;
  pollGamepad();
  state.px = Math.max(0.05, Math.min(0.95, state.px + state.vx));
  state.py = Math.max(0.08, Math.min(0.92, state.py + state.vy));
  state.vx *= 0.8; state.vy *= 0.8;

  for(const b of state.bolts){ b.x += b.vx; b.y += b.vy; b.life--; }
  state.bolts = state.bolts.filter(b => b.life > 0);

  for(const e of state.enemies){
    const dx = state.px - e.x, dy = state.py - e.y, d = Math.hypot(dx, dy) || 1;
    e.x += (dx/d)*e.speed; e.y += (dy/d)*e.speed;
    if(d < 0.03 && state.frame % 25 === 0) state.shield = Math.max(0, state.shield - 8);
    for(const b of state.bolts){
      if(Math.hypot(b.x - e.x, b.y - e.y) < 0.03){ e.hp--; b.life = 0; state.score += 50; }
    }
  }
  state.enemies = state.enemies.filter(e => e.hp > 0);
  if(state.enemies.length === 0){ state.wave++; spawnWave(state.wave); }

  const w = canvas.width, h = canvas.height;
  ctx.fillStyle = '#070a12'; ctx.fillRect(0,0,w,h);
  ctx.strokeStyle = 'rgba(40,54,85,0.45)';
  for(let gx=0; gx<w; gx+=48){ ctx.beginPath(); ctx.moveTo(gx,0); ctx.lineTo(gx,h); ctx.stroke(); }
  for(let gy=0; gy<h; gy+=48){ ctx.beginPath(); ctx.moveTo(0,gy); ctx.lineTo(w,gy); ctx.stroke(); }

  ctx.fillStyle = '$secondaryHex';
  for(const e of state.enemies){ ctx.beginPath(); ctx.arc(e.x*w, e.y*h, 11, 0, Math.PI*2); ctx.fill(); }
  ctx.fillStyle = '#ffeb3b';
  for(const b of state.bolts){ ctx.beginPath(); ctx.arc(b.x*w, b.y*h, 5, 0, Math.PI*2); ctx.fill(); }

  ctx.strokeStyle = '$accentHex'; ctx.lineWidth = 3;
  ctx.beginPath(); ctx.arc(state.px*w, state.py*h, 15, 0, Math.PI*2); ctx.stroke();
  statsEl.textContent = 'Score: '+state.score+' | Wave: '+state.wave+' | Shield: '+state.shield+' | Input: '+state.padName;
  requestAnimationFrame(tick);
}
tick();
</script>
</body>
</html>"""
  }

  /**
   * Standalone interactive Game Collection Archive & Explorer HTML.
   * Works completely offline in any browser (Samsung Internet, Chrome, Safari, PC, Console)
   * without requiring Google Sign-In or installing the Android app.
   */
  fun generateCollectionPortalHtml(
    profile: OperatorProfile,
    armory: List<ArmoryItem>,
    rigs: List<RigAsset>,
    matchLogs: List<MatchRecord> = emptyList(),
    linkedAccounts: List<LinkedAccount> = emptyList(),
  ): String {
    val jsonBundle = generatePortableJsonBundle(profile, armory, rigs, matchLogs, linkedAccounts)
    val safeJsonForScript = jsonBundle.replace("</", "<\\/")
    val safeCallsign = escapeHtml(profile.callsign)
    val world = UniverseCatalog.worldById(profile.activeUniverseId)

    return """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width,initial-scale=1" />
<title>The Unknown Universes — External Collection Vault ($safeCallsign)</title>
<style>
  :root { --bg: #070a12; --surface: #10182b; --border: #253454; --accent: #00e5ff; --gold: #ffd54f; --green: #00e676; --text: #eef3ff; --muted: #9eb0d0; }
  * { box-sizing: border-box; }
  body { margin: 0; background: var(--bg); color: var(--text); font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; line-height: 1.5; }
  .wrap { max-width: 1020px; margin: 0 auto; padding: 20px 16px 48px; }
  .hero { background: linear-gradient(135deg, #0d1629, #151c36); border: 1px solid var(--accent); border-radius: 16px; padding: 20px; margin-bottom: 18px; }
  .hero h1 { margin: 0 0 6px; font-size: 22px; color: var(--accent); letter-spacing: .5px; }
  .meta { color: var(--muted); font-size: 13px; margin-bottom: 14px; }
  .actions { display: flex; flex-wrap: wrap; gap: 10px; }
  .btn { background: var(--accent); color: #041016; border: none; border-radius: 10px; padding: 10px 14px; font-weight: 700; cursor: pointer; font-size: 13px; }
  .btn-outline { background: transparent; color: var(--text); border: 1px solid var(--border); }
  .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px; margin-bottom: 20px; }
  .stat { background: var(--surface); border: 1px solid var(--border); border-radius: 12px; padding: 14px; }
  .stat label { display: block; font-size: 11px; text-transform: uppercase; color: var(--muted); }
  .stat strong { font-size: 20px; color: var(--gold); }
  .tabs { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 14px; }
  .tab { background: var(--surface); color: var(--muted); border: 1px solid var(--border); border-radius: 999px; padding: 8px 14px; cursor: pointer; font-size: 13px; font-weight: 600; }
  .tab.active { background: rgba(0,229,255,.15); color: var(--accent); border-color: var(--accent); }
  .panel { display: none; }
  .panel.active { display: block; }
  .card { background: var(--surface); border: 1px solid var(--border); border-radius: 12px; padding: 14px; margin-bottom: 10px; }
  .card h3 { margin: 0 0 4px; font-size: 16px; }
  .pill { display: inline-block; padding: 2px 8px; border-radius: 999px; font-size: 11px; border: 1px solid var(--border); color: var(--accent); margin-right: 6px; }
  pre { background: #04070e; border: 1px solid var(--border); border-radius: 12px; padding: 12px; overflow-x: auto; font-size: 12px; color: #8be9fd; }
</style>
</head>
<body>
<div class="wrap">
  <div class="hero">
    <h1>THE UNKNOWN UNIVERSES — PORTABLE COLLECTION VAULT</h1>
    <div class="meta">Operator: <strong>$safeCallsign</strong> • Active World: <strong>${escapeHtml(world.title)}</strong> • Offline Snapshot (Zero Game / Google Lock-in)</div>
    <div class="actions">
      <button class="btn" onclick="downloadJson()">Download Restorable .JSON</button>
      <button class="btn btn-outline" onclick="copyJson()">Copy Collection JSON</button>
      <span id="copyMsg" style="color:var(--green);font-size:13px;align-self:center;"></span>
    </div>
  </div>

  <div class="grid">
    <div class="stat"><label>Essence Balance</label><strong>${profile.totalEssence}</strong></div>
    <div class="stat"><label>High Score</label><strong>${profile.highScore}</strong></div>
    <div class="stat"><label>Highest Wave</label><strong>Wave ${profile.highestWave}</strong></div>
    <div class="stat"><label>Saved Modules & Rigs</label><strong>${armory.size} Items • ${rigs.size} Rigs</strong></div>
  </div>

  <div class="tabs">
    <button class="tab active" onclick="showTab('worlds', this)">Worlds & Series (${UniverseCatalog.worlds.size})</button>
    <button class="tab" onclick="showTab('armory', this)">Armory (${armory.size})</button>
    <button class="tab" onclick="showTab('rigs', this)">ForgeRigs (${rigs.size})</button>
    <button class="tab" onclick="showTab('matches', this)">Match History (${matchLogs.size})</button>
    <button class="tab" onclick="showTab('accounts', this)">Linked Platforms (${linkedAccounts.size})</button>
    <button class="tab" onclick="showTab('raw', this)">Raw JSON</button>
  </div>

  <div id="tab-worlds" class="panel active"></div>
  <div id="tab-armory" class="panel"></div>
  <div id="tab-rigs" class="panel"></div>
  <div id="tab-matches" class="panel"></div>
  <div id="tab-accounts" class="panel"></div>
  <div id="tab-raw" class="panel"><pre id="rawJson"></pre></div>
</div>
<script>
const COLLECTION = $safeJsonForScript;

function esc(s){ return String(s ?? '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;'); }
function showTab(name, btn){
  document.querySelectorAll('.panel').forEach(p => p.classList.remove('active'));
  document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
  document.getElementById('tab-'+name).classList.add('active');
  btn.classList.add('active');
}
function render(){
  const worldsEl = document.getElementById('tab-worlds');
  worldsEl.innerHTML = (COLLECTION.worlds || []).map(w =>
    '<div class="card"><h3>'+esc(w.title)+'</h3><div class="meta">'+esc(w.inspirations)+'</div><div>'+esc(w.description)+'</div></div>'
  ).join('') + (COLLECTION.continuationSeries || []).map(s =>
    '<div class="card"><span class="pill">'+esc(s.sectorTag)+'</span><strong>'+esc(s.title)+'</strong></div>'
  ).join('');

  const armoryEl = document.getElementById('tab-armory');
  armoryEl.innerHTML = (COLLECTION.armory || []).map(a =>
    '<div class="card"><h3>'+esc(a.name)+' <span class="pill">'+esc(a.category).toUpperCase()+' T'+esc(a.tier)+'</span>'+(a.equipped?'<span class="pill" style="color:var(--green)">EQUIPPED</span>':'')+'</h3><div class="meta">'+esc(a.universeTag)+' • Power '+esc(a.powerValue)+' • CD '+esc(a.cooldownSec)+'s</div><div>'+esc(a.description || '')+'</div></div>'
  ).join('') || '<div class="card">No armory items saved.</div>';

  const rigsEl = document.getElementById('tab-rigs');
  rigsEl.innerHTML = (COLLECTION.rigs || []).map(r =>
    '<div class="card"><h3>'+esc(r.name)+' <span class="pill">'+esc(r.archetype)+'</span><span class="pill">'+esc(r.activeClip)+'</span></h3><div class="meta">Scale '+esc(r.rigScale)+'x • Speed '+esc(r.playbackSpeed)+'x • Keyframes: '+esc((r.keyframeFrames||[]).join(', '))+'</div></div>'
  ).join('') || '<div class="card">No custom rigs saved.</div>';

  const matchesEl = document.getElementById('tab-matches');
  matchesEl.innerHTML = (COLLECTION.matchLogs || []).map(m =>
    '<div class="card"><h3>['+esc(m.mode).toUpperCase()+'] '+esc(m.universeId)+' — Score '+esc(m.score)+'</h3><div class="meta">Wave '+esc(m.waveReached)+' • Eliminations '+esc(m.eliminations)+' • '+(m.extracted?'EXTRACTED':'COMBAT END')+'</div></div>'
  ).join('') || '<div class="card">No matches recorded yet.</div>';

  const accEl = document.getElementById('tab-accounts');
  accEl.innerHTML = (COLLECTION.linkedAccounts || []).map(l =>
    '<div class="card"><h3>'+esc(l.providerId).toUpperCase()+'</h3><div class="meta">Handle: '+esc(l.displayName)+' • Subject: '+esc(l.externalSubject)+'</div></div>'
  ).join('') || '<div class="card">No external platform accounts linked yet.</div>';

  document.getElementById('rawJson').textContent = JSON.stringify(COLLECTION, null, 2);
}
function downloadJson(){
  const blob = new Blob([JSON.stringify(COLLECTION, null, 2)], {type: 'application/json'});
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = 'unknown_universes_collection_backup.json';
  a.click();
}
function copyJson(){
  navigator.clipboard?.writeText(JSON.stringify(COLLECTION, null, 2));
  document.getElementById('copyMsg').textContent = 'Copied full JSON to clipboard!';
}
render();
</script>
</body>
</html>"""
  }

  /**
   * Generates a real Blender Python (bpy) rigging & keyframe animation script for ForgeRig.
   */
  fun generateBlenderRigScript(rig: RigAsset): String {
    val anglesJson =
      rig.poseAngles.entries.joinToString(",\n    ") { (k, v) -> "\"$k\": $v" }
    val framesStr = rig.keyframeFrames.joinToString(", ")
    return """# The Unknown Universes — ForgeRig Blender (bpy) Rig & Animation Script
# Compatible with Blender 3.6+ / 4.x
import bpy
import math

RIG_NAME = "${rig.name.replace("\"", "'")}"
ARCHETYPE = "${rig.archetype}"
ACTIVE_CLIP = "${rig.activeClip}"
IK_ENABLED = ${if (rig.ikEnabled) "True" else "False"}
RIG_SCALE = ${rig.rigScale}
KEYFRAMES = [$framesStr]

POSE_ANGLES_DEG = {
    $anglesJson
}

def build_unknown_universes_rig():
    bpy.ops.object.armature_add(enter_editmode=True, location=(0, 0, 0))
    arm_obj = bpy.context.active_object
    arm_obj.name = RIG_NAME
    arm_obj.scale = (RIG_SCALE, RIG_SCALE, RIG_SCALE)
    arm = arm_obj.data
    eb = arm.edit_bones

    root = eb[0]
    root.name = "torso"
    root.head = (0, 0, 0.9)
    root.tail = (0, 0, 1.45)

    def add_bone(name, head, tail, parent):
        b = eb.new(name)
        b.head = head
        b.tail = tail
        b.parent = parent
        return b

    add_bone("head", (0, 0, 1.48), (0, 0, 1.78), root)
    l_ua = add_bone("lUpperArm", (-0.24, 0, 1.40), (-0.48, 0, 1.15), root)
    add_bone("lForearm", (-0.48, 0, 1.15), (-0.65, 0, 0.90), l_ua)
    r_ua = add_bone("rUpperArm", (0.24, 0, 1.40), (0.48, 0, 1.15), root)
    r_fa = add_bone("rForearm", (0.48, 0, 1.15), (0.65, 0, 0.90), r_ua)
    add_bone("weaponAngle", (0.65, 0, 0.90), (0.85, 0.2, 0.90), r_fa)
    l_th = add_bone("lThigh", (-0.14, 0, 0.88), (-0.18, 0, 0.46), root)
    add_bone("lShin", (-0.18, 0, 0.46), (-0.20, 0, 0.05), l_th)
    r_th = add_bone("rThigh", (0.14, 0, 0.88), (0.18, 0, 0.46), root)
    add_bone("rShin", (0.18, 0, 0.46), (0.20, 0, 0.05), r_th)

    bpy.ops.object.mode_set(mode='POSE')
    for bone_name, deg in POSE_ANGLES_DEG.items():
        pb = arm_obj.pose.bones.get(bone_name)
        if pb:
            pb.rotation_mode = 'XYZ'
            pb.rotation_euler = (0.0, math.radians(deg), 0.0)
            for f in KEYFRAMES:
                pb.keyframe_insert(data_path="rotation_euler", frame=int(f))

    bpy.ops.object.mode_set(mode='OBJECT')
    print(f"Built {RIG_NAME} ({ARCHETYPE}) clip={ACTIVE_CLIP} IK={IK_ENABLED}")

if __name__ == "__main__":
    build_unknown_universes_rig()
"""
  }

  /**
   * Generates a complete portable JSON collection snapshot (schemaVersion = 2) usable completely
   * outside the app and Google, and restorable via Import Backup on any device.
   */
  fun generatePortableJsonBundle(
    profile: OperatorProfile,
    armory: List<ArmoryItem>,
    rigs: List<RigAsset>,
    matchLogs: List<MatchRecord> = emptyList(),
    linkedAccounts: List<LinkedAccount> = emptyList(),
  ): String {
    val root = JSONObject()
    root.put("schemaVersion", 2)
    root.put("project", "The Unknown Universes")
    root.put("packageName", BuildConfig.APPLICATION_ID)
    root.put("osEnvironment", "UnknownOS v${BuildConfig.VERSION_NAME}")
    root.put("exportedAtMillis", System.currentTimeMillis())

    // Top-level convenience metadata + full profile block
    root.put("callsign", profile.callsign)
    root.put("activeUniverse", profile.activeUniverseId)
    root.put("activeMode", profile.activeMode)
    root.put("activeRigId", profile.activeRigId)
    root.put("consolePreset", profile.consolePreset)
    root.put("totalEssence", profile.totalEssence)
    root.put("highScore", profile.highScore)
    root.put("highestWave", profile.highestWave)
    root.put("osWallpaperIndex", profile.osWallpaperIndex)

    root.put(
      "profile",
      JSONObject().apply {
        put("callsign", profile.callsign)
        put("activeUniverseId", profile.activeUniverseId)
        put("activeMode", profile.activeMode)
        put("activeRigId", profile.activeRigId)
        put("consolePreset", profile.consolePreset)
        put("totalEssence", profile.totalEssence)
        put("highScore", profile.highScore)
        put("highestWave", profile.highestWave)
        put("osWallpaperIndex", profile.osWallpaperIndex)
      },
    )

    val armoryArr = JSONArray()
    armory.forEach { item ->
      armoryArr.put(
        JSONObject().apply {
          put("id", item.id)
          put("name", item.name)
          put("category", item.category)
          put("universeTag", item.universeTag)
          put("description", item.description)
          put("tier", item.tier)
          put("maxTier", item.maxTier)
          put("powerValue", item.powerValue)
          put("cooldownSec", item.cooldownSec)
          put("accentColorHex", item.accentColorHex)
          put("equipped", item.equipped)
          put("isCustom", item.isCustom)
          put("createdAtMillis", item.createdAtMillis)
          put("updatedAtMillis", item.updatedAtMillis)
        }
      )
    }
    root.put("armory", armoryArr)

    val rigsArr = JSONArray()
    rigs.forEach { rig ->
      rigsArr.put(
        JSONObject().apply {
          put("id", rig.id)
          put("name", rig.name)
          put("archetype", rig.archetype)
          put("activeClip", rig.activeClip)
          put("ikEnabled", rig.ikEnabled)
          put("rigScale", rig.rigScale)
          put("playbackSpeed", rig.playbackSpeed)
          put("poseAngles", JSONObject(rig.poseAngles))
          put("keyframeFrames", JSONArray(rig.keyframeFrames))
          put("isCompleted", rig.isCompleted)
          put("createdAtMillis", rig.createdAtMillis)
          put("updatedAtMillis", rig.updatedAtMillis)
        }
      )
    }
    root.put("rigs", rigsArr)

    val matchArr = JSONArray()
    matchLogs.forEach { m ->
      matchArr.put(
        JSONObject().apply {
          put("id", m.id)
          put("universeId", m.universeId)
          put("mode", m.mode)
          put("score", m.score)
          put("waveReached", m.waveReached)
          put("eliminations", m.eliminations)
          put("extracted", m.extracted)
          put("durationSec", m.durationSec)
          put("createdAtMillis", m.createdAtMillis)
        }
      )
    }
    root.put("matchLogs", matchArr)

    val linkedArr = JSONArray()
    linkedAccounts.forEach { l ->
      linkedArr.put(
        JSONObject().apply {
          put("providerId", l.providerId)
          put("displayName", l.displayName)
          put("externalSubject", l.externalSubject)
          put("linkedAtMillis", l.linkedAtMillis)
        }
      )
    }
    root.put("linkedAccounts", linkedArr)

    val worldsArr = JSONArray()
    UniverseCatalog.worlds.forEach { w ->
      worldsArr.put(
        JSONObject().apply {
          put("id", w.id)
          put("title", w.title)
          put("inspirations", w.inspirations)
          put("description", w.description)
          put("defaultModeId", w.defaultModeId)
          put("platformBadges", w.platformBadges)
        }
      )
    }
    root.put("worlds", worldsArr)

    val seriesArr = JSONArray()
    OriginalVaultCatalog.continuationSeries.forEach { s ->
      seriesArr.put(
        JSONObject().apply {
          put("id", s.id)
          put("title", s.title)
          put("sectorTag", s.sectorTag)
          put("linkedWorldId", s.linkedWorldId)
          put("linkedModeId", s.linkedModeId)
        }
      )
    }
    root.put("continuationSeries", seriesArr)
    root.put("vaultArtworksCount", OriginalVaultCatalog.artworks.size)
    root.put("vaultVideosCount", OriginalVaultCatalog.videos.size)
    return root.toString(2)
  }

  /**
   * Parses any exported JSON collection bundle (v1 or v2) back into domain objects for import/restore.
   */
  fun parsePortableJsonBundle(jsonText: String): Result<PortableCollectionBundle> {
    return try {
      val root = JSONObject(jsonText)
      val prof = root.optJSONObject("profile")
      val now = System.currentTimeMillis()

      val callsign = (prof?.optString("callsign").takeUnless { it.isNullOrBlank() }
        ?: root.optString("callsign").ifBlank { "VanguardPrime" })
        .trim().take(30).let { if (it.length >= 2) it else it.padEnd(2, '_') }

      val activeUniverse = prof?.optString("activeUniverseId").takeUnless { it.isNullOrBlank() }
        ?: root.optString("activeUniverse").ifBlank { "sector_zero" }
      val activeMode = prof?.optString("activeMode").takeUnless { it.isNullOrBlank() }
        ?: root.optString("activeMode").ifBlank { "zombies" }
      val activeRigId = prof?.optString("activeRigId").takeUnless { it.isNullOrBlank() }
        ?: root.optString("activeRigId").ifBlank { "rig_operator" }
      val consolePreset = prof?.optString("consolePreset").takeUnless { it.isNullOrBlank() }
        ?: root.optString("consolePreset").ifBlank { "universal_web" }
      val totalEssence = (prof?.optLong("totalEssence", -1L)?.takeIf { it >= 0L }
        ?: root.optLong("totalEssence", 1500L)).coerceIn(0L, 999_999_999L)
      val highScore = (prof?.optLong("highScore", -1L)?.takeIf { it >= 0L }
        ?: root.optLong("highScore", 0L)).coerceIn(0L, 999_999_999L)
      val highestWave = (prof?.optLong("highestWave", -1L)?.takeIf { it >= 0L }
        ?: root.optLong("highestWave", 0L)).coerceIn(0L, 9999L)
      val osWallpaper = (prof?.optLong("osWallpaperIndex", -1L)?.takeIf { it >= 0L }
        ?: root.optLong("osWallpaperIndex", 0L)).coerceIn(0L, 10L)

      val armory = mutableListOf<ArmoryItem>()
      val armoryArr = root.optJSONArray("armory") ?: JSONArray()
      for (i in 0 until armoryArr.length()) {
        val obj = armoryArr.optJSONObject(i) ?: continue
        val name = obj.optString("name").trim().take(50)
        if (name.isEmpty()) continue
        val itemId = obj.optString("id").ifBlank { "import_armory_$i" }.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val maxTier = obj.optLong("maxTier", 5L).coerceIn(1L, 5L)
        val tier = obj.optLong("tier", 1L).coerceIn(1L, maxTier)
        armory.add(
          ArmoryItem(
            id = itemId,
            userId = "",
            name = name,
            category = obj.optString("category").ifBlank { "perk" },
            universeTag = obj.optString("universeTag").ifBlank { "UnknownOS" }.take(40),
            description = obj.optString("description").take(240),
            tier = tier,
            maxTier = maxTier,
            cooldownSec = obj.optLong("cooldownSec", 0L).coerceIn(0L, 120L),
            powerValue = obj.optLong("powerValue", 100L).coerceIn(1L, 500L),
            accentColorHex = obj.optString("accentColorHex").ifBlank { "#00E5FF" }.take(10),
            equipped = obj.optBoolean("equipped", false),
            isCustom = obj.optBoolean("isCustom", true),
            createdAtMillis = obj.optLong("createdAtMillis", now + i),
            updatedAtMillis = obj.optLong("updatedAtMillis", now + i),
          )
        )
      }

      val rigs = mutableListOf<RigAsset>()
      val rigsArr = root.optJSONArray("rigs") ?: JSONArray()
      for (i in 0 until rigsArr.length()) {
        val obj = rigsArr.optJSONObject(i) ?: continue
        val name = obj.optString("name").trim().take(50)
        if (name.isEmpty()) continue
        val rigId = obj.optString("id").ifBlank { "import_rig_$i" }.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val poseObj = obj.optJSONObject("poseAngles")
        val poseMap = linkedMapOf<String, Double>()
        poseObj?.keys()?.forEach { key ->
          poseMap[key] = poseObj.optDouble(key, 0.0)
        }
        val framesArr = obj.optJSONArray("keyframeFrames")
        val frames = mutableListOf<Long>()
        if (framesArr != null) {
          for (j in 0 until minOf(framesArr.length(), 20)) {
            frames.add(framesArr.optLong(j, 0L))
          }
        }
        rigs.add(
          RigAsset(
            id = rigId,
            userId = "",
            name = name,
            archetype = obj.optString("archetype").ifBlank { "operator" },
            activeClip = obj.optString("activeClip").ifBlank { "idle" },
            ikEnabled = obj.optBoolean("ikEnabled", true),
            rigScale = obj.optDouble("rigScale", 1.0).coerceIn(0.5, 2.0),
            playbackSpeed = obj.optDouble("playbackSpeed", 1.0).coerceIn(0.25, 3.0),
            poseAngles = if (poseMap.isEmpty()) UniverseCatalog.defaultPoseMap() else poseMap,
            keyframeFrames = if (frames.isEmpty()) listOf(0L, 15L, 30L, 45L, 60L) else frames,
            isCompleted = obj.optBoolean("isCompleted", true),
            createdAtMillis = obj.optLong("createdAtMillis", now + i),
            updatedAtMillis = obj.optLong("updatedAtMillis", now + i),
          )
        )
      }

      val matches = mutableListOf<MatchRecord>()
      val matchArr = root.optJSONArray("matchLogs") ?: JSONArray()
      for (i in 0 until matchArr.length()) {
        val obj = matchArr.optJSONObject(i) ?: continue
        val matchId = obj.optString("id").ifBlank { "import_match_${now}_$i" }.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        matches.add(
          MatchRecord(
            id = matchId,
            userId = "",
            universeId = obj.optString("universeId").ifBlank { "sector_zero" },
            mode = obj.optString("mode").ifBlank { "zombies" },
            score = obj.optLong("score", 0L).coerceIn(0L, 999_999_999L),
            waveReached = obj.optLong("waveReached", 1L).coerceIn(0L, 9999L),
            eliminations = obj.optLong("eliminations", 0L).coerceIn(0L, 99999L),
            extracted = obj.optBoolean("extracted", false),
            durationSec = obj.optLong("durationSec", 0L).coerceIn(0L, 86400L),
            createdAtMillis = obj.optLong("createdAtMillis", now + i),
          )
        )
      }

      val linked = mutableListOf<LinkedAccount>()
      val linkedArr = root.optJSONArray("linkedAccounts") ?: JSONArray()
      for (i in 0 until linkedArr.length()) {
        val obj = linkedArr.optJSONObject(i) ?: continue
        val provider = obj.optString("providerId").trim()
        val display = obj.optString("displayName").trim().take(64)
        if (provider.isEmpty() || display.isEmpty()) continue
        linked.add(
          LinkedAccount(
            id = provider,
            userId = "",
            providerId = provider,
            displayName = display,
            externalSubject = obj.optString("externalSubject").ifBlank { display }.take(128),
            linkedAtMillis = obj.optLong("linkedAtMillis", now + i),
          )
        )
      }

      Result.success(
        PortableCollectionBundle(
          callsign = callsign,
          activeUniverseId = activeUniverse,
          activeMode = activeMode,
          activeRigId = activeRigId,
          consolePreset = consolePreset,
          totalEssence = totalEssence,
          highScore = highScore,
          highestWave = highestWave,
          osWallpaperIndex = osWallpaper,
          armory = armory,
          rigs = rigs,
          matchLogs = matches,
          linkedAccounts = linked,
        )
      )
    } catch (e: Exception) {
      Result.failure(IllegalArgumentException("Invalid collection JSON file: ${e.localizedMessage}"))
    }
  }

  private fun escapeHtml(raw: String): String =
    raw
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
}
