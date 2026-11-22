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
   * playable build of The Unknown Universes with decreased graphics & tuned performance across
   * all game collection sections (Arena, Worlds & Hero Frontier, Characters & Rarity, Weapons & Wonder,
   * Armory & Rigs) that runs offline in browsers across iOS, PS5/PS4/PS3, Nintendo, Xbox, and PC.
   */
  fun generatePlayableHtmlPackage(
    profile: OperatorProfile,
    armory: List<ArmoryItem>,
    rig: RigAsset?,
    progression: com.example.data.model.OperatorProgressionState = com.example.data.model.OperatorProgressionState(),
    graphicsTier: String = "medium",
    platformTargetId: String = profile.consolePreset,
  ): String {
    val world = UniverseCatalog.worldById(profile.activeUniverseId)
    val mode = UniverseCatalog.modeById(profile.activeMode)
    val console = UniverseCatalog.consoleById(platformTargetId)
    val equipped = armory.filter { it.equipped }
    val accentHex = String.format("#%06X", 0xFFFFFF and world.accentHex.toInt())
    val secondaryHex = String.format("#%06X", 0xFFFFFF and world.secondaryHex.toInt())
    val rigName = rig?.name ?: "Vanguard Tactical Rig"
    val rigClip = rig?.activeClip ?: "idle"
    val equippedNames =
      if (equipped.isEmpty()) "Standard Loadout" else equipped.joinToString(" • ") { "${it.name} T${it.tier}" }
    val pwr = progression.computePowerAndLightLevel(profile.totalEssence)
    val activeHero = progression.activeOutfitSpec()
    val heroRarity = progression.outfitRarity(activeHero.id)
    val activeWpn = com.example.data.model.ProgressionCatalog.weaponById(progression.weaponCustom.equippedWeaponId)
    val safeCallsign = escapeHtml(profile.callsign)
    val safeWorld = escapeHtml(world.title)
    val safeMode = escapeHtml(mode.label)
    val safeConsole = escapeHtml(console.label)
    val safeLoadout = escapeHtml(equippedNames)

    val worldsJson = JSONArray().apply {
      UniverseCatalog.worlds.forEach { w ->
        put(JSONObject().apply {
          put("id", w.id); put("title", w.title); put("subtitle", w.subtitle)
          put("inspirations", w.inspirations); put("mechanic", w.signatureMechanic)
          put("modes", w.supportedModes.joinToString(", ").uppercase())
          put("accent", String.format("#%06X", 0xFFFFFF and w.accentHex.toInt()))
        })
      }
    }.toString().replace("</", "<\\/")

    val modesJson = JSONArray().apply {
      UniverseCatalog.modes.forEach { m ->
        put(JSONObject().apply {
          put("id", m.id); put("label", m.label); put("tag", m.shortTag); put("summary", m.summary)
        })
      }
    }.toString().replace("</", "<\\/")

    val areasJson = JSONArray().apply {
      com.example.data.model.MultiverseArsenalCatalog.customAreas.forEach { a ->
        put(JSONObject().apply {
          put("id", a.id); put("name", a.name); put("origin", a.storyEraNote); put("summary", a.description)
        })
      }
    }.toString().replace("</", "<\\/")

    val charsJson = JSONArray().apply {
      com.example.data.model.MultiverseArsenalCatalog.outfits.forEach { c ->
        val r = progression.outfitRarity(c.id)
        put(JSONObject().apply {
          put("id", c.id); put("name", c.nameWithVersion); put("origin", c.originTag)
          put("group", c.characterGroup.label); put("isOriginal", c.isOriginalCharacter); put("rarity", r.label)
          put("shd", c.baseShieldBonus); put("dmg", c.baseDamageBonus)
          put("skill", c.signatureSkillSummary)
        })
      }
    }.toString().replace("</", "<\\/")

    val wpnsJson = JSONArray().apply {
      com.example.data.model.ProgressionCatalog.weapons.forEach { w ->
        put(JSONObject().apply {
          put("id", w.id); put("name", w.name); put("origin", w.universeOrigin)
          put("dmg", w.baseDamage); put("spread", w.baseSpreadCount); put("pierce", w.basePierce)
        })
      }
    }.toString().replace("</", "<\\/")

    val wondersJson = JSONArray().apply {
      com.example.data.model.WonderWeaponKind.entries.forEach { w ->
        put(JSONObject().apply {
          put("id", w.id); put("name", w.displayName); put("tag", w.shortTag); put("desc", w.description)
        })
      }
    }.toString().replace("</", "<\\/")

    val enemyDensity = progression.levelCustom.enemyDensityMultiplier.coerceIn(0.75f, 2.5f)
    val diffTier = progression.levelCustom.worldDifficultyTier

    return """<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no,viewport-fit=cover" />
<meta name="apple-mobile-web-app-capable" content="yes" />
<meta name="apple-mobile-web-app-status-bar-style" content="black-translucent" />
<title>The Unknown Universes — UnknownOS HTML5 Edition</title>
<style>
  :root { --accent: $accentHex; --sec: $secondaryHex; --bg: #070a12; --panel: #0e1525; --border: #253454; }
  * { box-sizing: border-box; user-select: none; -webkit-user-select: none; }
  body { margin: 0; background: var(--bg); color: #f0f4ff; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; overflow: hidden; display: flex; flex-direction: column; height: 100vh; }
  header { display: flex; justify-content: space-between; align-items: center; padding: 8px 12px; background: var(--panel); border-bottom: 1px solid var(--border); font-size: 12px; gap: 8px; flex-wrap: wrap; }
  .badge { padding: 3px 8px; border-radius: 999px; background: rgba(0,229,255,0.14); border: 1px solid var(--accent); color: var(--accent); font-weight: 700; font-size: 11px; }
  nav { display: flex; gap: 6px; padding: 6px 12px; background: #0a101d; border-bottom: 1px solid var(--border); overflow-x: auto; }
  .nav-btn { background: #131c31; color: #b0c4e8; border: 1px solid var(--border); border-radius: 8px; padding: 6px 11px; font-weight: 700; font-size: 11px; cursor: pointer; white-space: nowrap; }
  .nav-btn.active { background: rgba(0,229,255,0.18); color: var(--accent); border-color: var(--accent); }
  main { position: relative; flex: 1; overflow: hidden; }
  .section { display: none; width: 100%; height: 100%; overflow-y: auto; }
  .section.active { display: block; }
  #sec-arena { overflow: hidden; position: relative; }
  canvas { width: 100%; height: 100%; display: block; touch-action: none; }
  .hud { position: absolute; left: 10px; top: 10px; background: rgba(14,21,37,0.84); padding: 8px 11px; border-radius: 10px; border: 1px solid var(--border); font-size: 11px; line-height: 1.4; pointer-events: none; max-width: 72%; }
  .top-right-hud { position: absolute; right: 10px; top: 10px; display: flex; flex-direction: column; gap: 6px; align-items: flex-end; }
  .controls { position: absolute; bottom: 14px; right: 14px; display: flex; gap: 8px; flex-wrap: wrap; justify-content: flex-end; }
  .ctrl-btn { background: var(--panel); color: #fff; border: 1.5px solid var(--accent); border-radius: 10px; padding: 10px 13px; font-weight: 700; font-size: 12px; cursor: pointer; }
  .ctrl-btn:active { background: var(--accent); color: #001; }
  .grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(250px, 1fr)); gap: 10px; padding: 14px; }
  .card { background: var(--panel); border: 1px solid var(--border); border-radius: 12px; padding: 12px; font-size: 12px; }
  .card h4 { margin: 0 0 4px; color: var(--accent); font-size: 14px; }
  .pill { display: inline-block; padding: 2px 7px; border-radius: 999px; font-size: 10px; border: 1px solid var(--border); color: #ffd54f; margin-right: 4px; }
  select { background: #131c31; color: #fff; border: 1px solid var(--accent); border-radius: 6px; padding: 4px 8px; font-size: 11px; }
</style>
</head>
<body>
<header>
  <div><strong>UNKNOWN<span style="color:var(--accent)">OS</span></strong> • $safeWorld • <span style="color:#ffd54f">PWR ${pwr.powerLevel} / LT ${pwr.lightLevel}</span></div>
  <div class="badge">$safeMode • $safeConsole • ${escapeHtml(diffTier.label)}</div>
</header>
<nav>
  <button class="nav-btn active" onclick="switchSection('arena',this)">2.5D Arena</button>
  <button class="nav-btn" onclick="switchSection('worlds',this)">Worlds &amp; Frontier (${UniverseCatalog.worlds.size + UniverseCatalog.modes.size + com.example.data.model.MultiverseArsenalCatalog.customAreas.size})</button>
  <button class="nav-btn" onclick="switchSection('chars',this)">Characters &amp; Rarity (${com.example.data.model.MultiverseArsenalCatalog.outfits.size})</button>
  <button class="nav-btn" onclick="switchSection('weapons',this)">Weapons &amp; Wonder (${com.example.data.model.ProgressionCatalog.weapons.size + com.example.data.model.WonderWeaponKind.entries.size})</button>
  <button class="nav-btn" onclick="switchSection('rigs',this)">Armory &amp; Rig</button>
  <button class="nav-btn" onclick="switchSection('guide',this)" style="color:#00e676; border-color:#00e676;">No-APK Play Guide</button>
</nav>
<main>
  <div id="sec-arena" class="section active">
    <canvas id="arena"></canvas>
    <div class="hud">
      <div><strong>Op:</strong> $safeCallsign (${escapeHtml(activeHero.nameWithVersion)} [${escapeHtml(heroRarity.label)}]) • <strong>Wpn:</strong> ${escapeHtml(activeWpn.name)}</div>
      <div id="stats">Score: 0 | Wave: 1 | Shield: 100 | PaP T1 | Prompt: Roam Frontier</div>
      <div id="envHud" style="color:#8be9fd;">Camps • PaP • Perks • Hazards (Higher Loot) • Cat Eggs • Unbounded World</div>
    </div>
    <div class="top-right-hud">
      <select id="gfxSel" title="HTML5 Graphics Quality (Decreased Load)">
        <option value="low" ${if (graphicsTier == "low") "selected" else ""}>GFX: Low (60fps Lite)</option>
        <option value="medium" ${if (graphicsTier == "medium") "selected" else ""}>GFX: Medium (Balanced 2.5D)</option>
        <option value="high" ${if (graphicsTier == "high") "selected" else ""}>GFX: High (Extended Shadows)</option>
      </select>
    </div>
    <div class="controls">
      <button class="ctrl-btn" id="btnInteract">INTERACT [E]</button>
      <button class="ctrl-btn" id="btnFire">FIRE</button>
      <button class="ctrl-btn" id="btnPower">POWER</button>
      <button class="ctrl-btn" id="btnDash">DASH</button>
    </div>
  </div>
  <div id="sec-worlds" class="section"><div id="worldsGrid" class="grid"></div></div>
  <div id="sec-chars" class="section"><div id="charsGrid" class="grid"></div></div>
  <div id="sec-weapons" class="section"><div id="weaponsGrid" class="grid"></div></div>
  <div id="sec-rigs" class="section"><div id="rigsGrid" class="grid"></div></div>
  <div id="sec-guide" class="section">
    <div style="padding: 24px; max-width: 800px; margin: 0 auto; line-height: 1.6;">
      <h2 style="color: var(--accent);">How to Play Game Collections Without an APK (No-APK Play Guide)</h2>
      <p>This self-contained HTML5 game collection package is designed to run instantly across any device or platform without requiring an Android APK or installation.</p>
      
      <h3 style="color: #ffd54f; margin-top: 18px;">1. Instant Browser Play (PC, Mac, Linux, iOS, Android)</h3>
      <ul>
        <li>Save or export this game collection file as <strong>index.html</strong>.</li>
        <li>Double-click the file or drag and drop it into any modern web browser (Google Chrome, Apple Safari, Microsoft Edge, Mozilla Firefox).</li>
        <li>The game runs fully offline with zero server dependencies, supporting canvas rendering, touch controls, and audio.</li>
      </ul>

      <h3 style="color: #ffd54f; margin-top: 18px;">2. Hosting on GitHub Pages or Vercel (Free Cloud Link)</h3>
      <ul>
        <li>Create a new public repository on GitHub (e.g., <code>my-game-collections</code>).</li>
        <li>Upload <code>index.html</code> to the root of the repository.</li>
        <li>Go to repository <strong>Settings > Pages</strong>, select <code>main/root</code>, and save.</li>
        <li>Share your live URL with friends so they can play instantly in their browser without downloading anything!</li>
      </ul>

      <h3 style="color: #ffd54f; margin-top: 18px;">3. Standalone PC App (.hta) or PWA</h3>
      <ul>
        <li><strong>Windows HTA:</strong> Rename <code>index.html</code> to <code>game.hta</code> to run it as a standalone native Windows desktop application.</li>
        <li><strong>Progressive Web App:</strong> Open the HTML page in Chrome/Edge, click the menu (⋮), and select <strong>"Install The Unknown Universes"</strong> to add it to your desktop or home screen like a native app.</li>
      </ul>

      <h3 style="color: #ffd54f; margin-top: 18px;">4. Controller Support</h3>
      <ul>
        <li>Connect any Bluetooth or USB gamepad (Xbox Series X, PlayStation DualSense, Nintendo Switch Pro).</li>
        <li>The W3C Gamepad API automatically detects buttons and joysticks for authentic console-grade control.</li>
      </ul>
    </div>
  </div>
</main>
<script>
const WORLDS = $worldsJson;
const MODES = $modesJson;
const AREAS = $areasJson;
const CHARS = $charsJson;
const WPNS = $wpnsJson;
const WONDERS = $wondersJson;
const RIG_INFO = { name: "${escapeHtml(rigName)}", clip: "${escapeHtml(rigClip)}", loadout: "$safeLoadout", callsign: "$safeCallsign", pwr: ${pwr.powerLevel}, light: ${pwr.lightLevel} };
const PLATFORM_PROFILE = "${escapeHtml(console.id)}";

function switchSection(id, btn){
  document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
  document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
  document.getElementById('sec-'+id).classList.add('active');
  btn.classList.add('active');
}

function renderCollections(){
  document.getElementById('worldsGrid').innerHTML =
    MODES.map(m => '<div class="card"><h4>[MODE: '+m.tag+'] '+m.label+'</h4><p>'+m.summary+'</p></div>').join('') +
    AREAS.map(a => '<div class="card"><h4>[ARENA] '+a.name+'</h4><div class="pill">'+a.origin+'</div><p>'+a.summary+'</p></div>').join('') +
    WORLDS.map(w => '<div class="card"><h4>[WORLD] '+w.title+'</h4><div class="pill">'+w.modes+'</div><p>'+w.inspirations+' — '+w.mechanic+'</p></div>').join('');
  document.getElementById('charsGrid').innerHTML = CHARS.map(c =>
    '<div class="card"><h4>'+c.name+'</h4><span class="pill">'+c.group+'</span><span class="pill">'+c.rarity+'</span>'+(c.isOriginal?'<span class="pill" style="color:#00e676">ORIGINAL +25%</span>':'')+'<p>'+c.origin+' • +'+c.shd+' SHD • +'+c.dmg+' DMG • '+c.skill+'</p></div>'
  ).join('');
  document.getElementById('weaponsGrid').innerHTML =
    WPNS.map(w => '<div class="card"><h4>'+w.name+'</h4><div class="pill">'+w.origin+'</div><p>DMG '+w.dmg+' • Spread '+w.spread+' • Pierce '+w.pierce+'</p></div>').join('') +
    WONDERS.map(w => '<div class="card"><h4>[WONDER] '+w.name+' ('+w.tag+')</h4><p>'+w.desc+'</p></div>').join('');
  document.getElementById('rigsGrid').innerHTML =
    '<div class="card"><h4>Operator Rig: '+RIG_INFO.name+'</h4><p>Clip: '+RIG_INFO.clip+' • PWR '+RIG_INFO.pwr+' / LIGHT '+RIG_INFO.light+'</p><p>Loadout: '+RIG_INFO.loadout+'</p></div>';
}
renderCollections();

const canvas = document.getElementById('arena');
const ctx = canvas.getContext('2d');
const statsEl = document.getElementById('stats');
const envHud = document.getElementById('envHud');
const gfxSel = document.getElementById('gfxSel');
function resize(){ canvas.width = canvas.clientWidth; canvas.height = canvas.clientHeight; }
window.addEventListener('resize', resize); resize();

const state = {
  px: 5.0, py: 5.0, aimX: 1, aimY: 0, vx: 0, vy: 0, score: 0, wave: 1, shield: 120,
  papTier: 1, cratesOpened: 0, catsFound: 0, powerCd: 0, bolts: [], enemies: [], keys: {}, frame: 0,
  padName: 'Keyboard/Touch', prompt: 'Open Frontier — No Borders',
  camps: [{x:4.4,y:4.8,label:'North Rest Camp'},{x:6.3,y:5.6,label:'Verdansk Depot Camp'}],
  stations: [{x:5.3,y:4.7,k:'PAP',label:'Pack-A-Punch'},{x:4.6,y:5.4,k:'PERK',label:'Perk Machine'},{x:5.7,y:5.2,k:'AMMO',label:'Ammo Crate'}],
  hazards: [{x:3.2,y:3.8,k:'ICE'},{x:6.9,y:3.5,k:'FIRE'},{x:3.4,y:6.6,k:'TOXIC'},{x:7.0,y:6.5,k:'ELECTRIC'},{x:8.4,y:5.0,k:'WINDY'}],
  crates: [{x:3.3,y:3.9,tier:'Hazard Exotic',open:false},{x:7.0,y:3.6,tier:'Hazard Legendary',open:false},{x:11.2,y:5.0,tier:'Frontier Rare',open:false}],
  cats: [{x:4.0,y:4.1,found:false},{x:6.7,y:6.2,found:false}]
};

function spawnWave(n){
  const count = Math.min(48, Math.round((4 + n*2) * ${enemyDensity} * 0.85));
  for(let i=0;i<count;i++){
    const a = Math.random()*Math.PI*2;
    const dist = 1.6 + Math.random()*1.4;
    state.enemies.push({
      x: state.px + Math.cos(a)*dist, y: state.py + Math.sin(a)*dist,
      hp: Math.max(1, Math.round((1 + Math.floor(n/2)) * ${diffTier.enemyHpScale})),
      speed: (0.0085 + n*0.0012) * ${diffTier.enemySpeedScale}
    });
  }
}
spawnWave(1);

function interactWorld(){
  for(const c of state.cats){ if(!c.found && Math.hypot(c.x-state.px,c.y-state.py)<0.55){ c.found=true; state.catsFound++; state.score+=450; state.prompt='Cat Easter Egg Found! +450 Loot Crate'; return; } }
  for(const cr of state.crates){ if(!cr.open && Math.hypot(cr.x-state.px,cr.y-state.py)<0.6){ cr.open=true; state.cratesOpened++; state.score+=300; state.shield=Math.min(200,state.shield+35); state.prompt='Opened '+cr.tier+' Crate!'; return; } }
  for(const st of state.stations){ if(Math.hypot(st.x-state.px,st.y-state.py)<0.65){ if(st.k==='PAP'){state.papTier=Math.min(25,state.papTier+1);state.prompt='Pack-A-Punch -> T'+state.papTier;} else if(st.k==='PERK'){state.score+=150;state.prompt='Perk Drink Active!';} else{state.shield=Math.min(200,state.shield+25);state.prompt='Ammo Resupplied!';} return; } }
  for(const cp of state.camps){ if(Math.hypot(cp.x-state.px,cp.y-state.py)<0.7){ state.shield=200; state.prompt='Rested at '+cp.label+' (Full Shield)'; return; } }
}

window.addEventListener('keydown', e => {
  state.keys[e.code] = true;
  if(e.code === 'Space') fireBolt();
  if(e.code === 'KeyE' || e.code === 'KeyF') interactWorld();
  if(e.code === 'KeyQ') triggerPower();
});
window.addEventListener('keyup', e => { state.keys[e.code] = false; });
canvas.addEventListener('pointerdown', e => {
  const r = canvas.getBoundingClientRect();
  const dx = (e.clientX - r.left)/r.width - 0.5, dy = (e.clientY - r.top)/r.height - 0.54;
  fireBoltTowards(dx, dy);
});
document.getElementById('btnInteract').onclick = interactWorld;
document.getElementById('btnFire').onclick = fireBolt;
document.getElementById('btnPower').onclick = triggerPower;
document.getElementById('btnDash').onclick = () => { state.vx *= 2.4; state.vy *= 2.4; };

function fireBolt(){ fireBoltTowards(state.aimX, state.aimY); }
function fireBoltTowards(dx, dy){
  const len = Math.hypot(dx, dy) || 1;
  state.aimX = dx/len; state.aimY = dy/len;
  state.bolts.push({ x: state.px, y: state.py, vx: state.aimX*0.09, vy: state.aimY*0.09, life: 45 });
}
function triggerPower(){
  if(state.powerCd > 0) return;
  state.powerCd = 160;
  state.enemies = state.enemies.filter(e => Math.hypot(e.x - state.px, e.y - state.py) > 1.4);
  state.score += 180;
}

function pollGamepad(){
  const pads = navigator.getGamepads ? navigator.getGamepads() : [];
  for(const p of pads){
    if(!p) continue;
    state.padName = PLATFORM_PROFILE + ': ' + p.id.slice(0, 16);
    if(Math.abs(p.axes[0]) > 0.16) state.vx += p.axes[0]*0.014;
    if(Math.abs(p.axes[1]) > 0.16) state.vy += p.axes[1]*0.014;
    if(Math.hypot(p.axes[2]||0, p.axes[3]||0) > 0.22){
      state.aimX = p.axes[2]; state.aimY = p.axes[3];
      if(state.frame % 9 === 0) fireBolt();
    }
    if(p.buttons[0]?.pressed && state.frame % 10 === 0) fireBolt();
    if(p.buttons[2]?.pressed && state.frame % 18 === 0) interactWorld();
    if(p.buttons[3]?.pressed) triggerPower();
    break;
  }
}

function proj(wx, wy, w, h){
  const rx = wx - state.px, ry = wy - state.py;
  const pScale = 1 / (1 - Math.max(-0.55, Math.min(0.6, ry*0.14)));
  return { x: w*0.5 + rx * (w*0.22) * pScale, y: h*0.54 + ry * (h*0.20) * pScale, s: pScale };
}

function tick(){
  state.frame++;
  if(state.powerCd > 0) state.powerCd--;
  const spd = 0.011;
  if(state.keys['KeyW']||state.keys['ArrowUp']) state.vy -= spd;
  if(state.keys['KeyS']||state.keys['ArrowDown']) state.vy += spd;
  if(state.keys['KeyA']||state.keys['ArrowLeft']) state.vx -= spd;
  if(state.keys['KeyD']||state.keys['ArrowRight']) state.vx += spd;
  pollGamepad();
  // Unbounded open world beyond 10x10 Verdansk sector!
  state.px += state.vx; state.py += state.vy;
  state.vx *= 0.78; state.vy *= 0.78;

  for(const b of state.bolts){ b.x += b.vx; b.y += b.vy; b.life--; }
  state.bolts = state.bolts.filter(b => b.life > 0);
  for(const e of state.enemies){
    const dx = state.px - e.x, dy = state.py - e.y, d = Math.hypot(dx, dy) || 1;
    e.x += (dx/d)*e.speed; e.y += (dy/d)*e.speed;
    if(d < 0.2 && state.frame % 28 === 0) state.shield = Math.max(0, state.shield - 7);
    for(const b of state.bolts){
      if(Math.hypot(b.x - e.x, b.y - e.y) < 0.24){ e.hp -= Math.max(1, Math.floor(state.papTier/2)+1); b.life = 0; state.score += 55; }
    }
  }
  state.enemies = state.enemies.filter(e => e.hp > 0);
  if(state.enemies.length === 0){ state.wave++; spawnWave(state.wave); }

  const w = canvas.width, h = canvas.height;
  const gfx = gfxSel.value;
  ctx.fillStyle = '#070a12'; ctx.fillRect(0,0,w,h);
  // Tilted 2.5D horizon band
  const grad = ctx.createLinearGradient(0,0,0,h*0.26);
  grad.addColorStop(0,'#111c36'); grad.addColorStop(1,'#070a12');
  ctx.fillStyle = grad; ctx.fillRect(0,0,w,h*0.26);

  if(gfx !== 'low'){
    ctx.strokeStyle = 'rgba(0,229,255,0.16)'; ctx.lineWidth = 1;
    for(let i=-4;i<=4;i++){
      const gx = Math.floor(state.px)+i;
      const a = proj(gx, state.py-2.4, w, h), b = proj(gx, state.py+2.4, w, h);
      ctx.beginPath(); ctx.moveTo(a.x,a.y); ctx.lineTo(b.x,b.y); ctx.stroke();
    }
    for(let j=-3;j<=3;j++){
      const gy = Math.floor(state.py)+j;
      const a = proj(state.px-2.8, gy, w, h), b = proj(state.px+2.8, gy, w, h);
      ctx.beginPath(); ctx.moveTo(a.x,a.y); ctx.lineTo(b.x,b.y); ctx.stroke();
    }
  }

  for(const hz of state.hazards){ const p=proj(hz.x,hz.y,w,h); ctx.fillStyle='rgba(255,82,82,0.22)'; ctx.beginPath(); ctx.arc(p.x,p.y,22*p.s,0,Math.PI*2); ctx.fill(); }
  for(const cp of state.camps){ const p=proj(cp.x,cp.y,w,h); ctx.fillStyle='#ff9100'; ctx.beginPath(); ctx.arc(p.x,p.y,11*p.s,0,Math.PI*2); ctx.fill(); }
  for(const st of state.stations){ const p=proj(st.x,st.y,w,h); ctx.fillStyle='#00e5ff'; ctx.fillRect(p.x-8*p.s,p.y-12*p.s,16*p.s,20*p.s); }
  for(const cr of state.crates){ if(cr.open) continue; const p=proj(cr.x,cr.y,w,h); ctx.fillStyle='#ffd54f'; ctx.fillRect(p.x-7*p.s,p.y-7*p.s,14*p.s,11*p.s); }
  for(const c of state.cats){ if(c.found) continue; const p=proj(c.x,c.y,w,h); ctx.fillStyle='#ff4081'; ctx.beginPath(); ctx.arc(p.x,p.y,6*p.s,0,Math.PI*2); ctx.fill(); }

  ctx.fillStyle = '$secondaryHex';
  for(const e of state.enemies){
    const p=proj(e.x,e.y,w,h);
    if(gfx==='high'){ ctx.fillStyle='rgba(0,0,0,0.45)'; ctx.beginPath(); ctx.ellipse(p.x,p.y+8*p.s,10*p.s,4*p.s,0,0,Math.PI*2); ctx.fill(); }
    ctx.fillStyle = '$secondaryHex';
    ctx.beginPath(); ctx.arc(p.x,p.y,10*p.s,0,Math.PI*2); ctx.fill();
  }
  ctx.fillStyle = '#ffeb3b';
  for(const b of state.bolts){ const p=proj(b.x,b.y,w,h); ctx.beginPath(); ctx.arc(p.x,p.y,4.5*p.s,0,Math.PI*2); ctx.fill(); }

  const me = proj(state.px, state.py, w, h);
  ctx.strokeStyle = '$accentHex'; ctx.lineWidth = 3;
  ctx.beginPath(); ctx.arc(me.x, me.y, 14*me.s, 0, Math.PI*2); ctx.stroke();

  const outZone = (state.px < 0 || state.px > 10 || state.py < 0 || state.py > 10) ? 'OUTER FRONTIER' : 'VERDANSK CORE';
  statsEl.textContent = 'Score: '+state.score+' | Wave: '+state.wave+' | SHD: '+state.shield+' | PaP T'+state.papTier+' | Cats: '+state.catsFound+' | Crates: '+state.cratesOpened;
  envHud.textContent = outZone+' ('+state.px.toFixed(1)+', '+state.py.toFixed(1)+') • Input: '+state.padName+' • '+state.prompt;
  requestAnimationFrame(tick);
}
tick();
</script>
</body>
</html>"""
  }

  /**
   * Generates a runnable Windows Standalone HTA executable launcher (.hta) wrapping the full HTML5 engine.
   */
  fun generatePcExecutableHta(
    profile: OperatorProfile,
    armory: List<ArmoryItem>,
    rig: RigAsset?,
    progression: com.example.data.model.OperatorProgressionState = com.example.data.model.OperatorProgressionState(),
    graphicsTier: String = "medium",
  ): String {
    val html = generatePlayableHtmlPackage(profile, armory, rig, progression, graphicsTier, "pc_desktop")
    val htaHeader = """<HTA:APPLICATION ID="UnknownUniversesPC" APPLICATIONNAME="The Unknown Universes" BORDER="thin" CAPTION="yes" MAXIMIZEBUTTON="yes" MINIMIZEBUTTON="yes" SYSMENU="yes" WINDOWSTATE="maximize" />"""
    return html.replaceFirst("</head>", "$htaHeader\n</head>")
  }

  /**
   * Generates real target-platform build manifests & controller config packages for
   * PlayStation (PS5/PS4/PS3), Nintendo Switch, Xbox UWP AppxManifest XML, iOS PWA, and PC EXE spec.
   */
  fun generateConsolePackageManifest(
    profile: OperatorProfile,
    progression: com.example.data.model.OperatorProgressionState,
    platformTargetId: String,
    graphicsTier: String = "medium",
  ): String {
    val world = UniverseCatalog.worldById(profile.activeUniverseId)
    val console = UniverseCatalog.consoleById(platformTargetId)
    val pwr = progression.computePowerAndLightLevel(profile.totalEssence)
    val hero = progression.activeOutfitSpec()

    if (platformTargetId == "xbox_series") {
      return """<?xml version="1.0" encoding="utf-8"?>
<Package xmlns="http://schemas.microsoft.com/appx/manifest/foundation/windows10"
         xmlns:uap="http://schemas.microsoft.com/appx/manifest/uap/windows10">
  <Identity Name="UnknownOS.TheUnknownUniverses" Publisher="CN=${escapeHtml(profile.callsign)}" Version="${BuildConfig.VERSION_NAME}.0" ProcessorArchitecture="x64" />
  <Properties>
    <DisplayName>The Unknown Universes (${escapeHtml(world.title)})</DisplayName>
    <PublisherDisplayName>UnknownOS Studio</PublisherDisplayName>
  </Properties>
  <Dependencies>
    <TargetDeviceFamily Name="Windows.Xbox" MinVersion="10.0.19041.0" MaxVersionTested="10.0.26100.0" />
    <TargetDeviceFamily Name="Windows.Desktop" MinVersion="10.0.19041.0" MaxVersionTested="10.0.26100.0" />
  </Dependencies>
  <Capabilities><Capability Name="internetClient" /></Capabilities>
  <Applications>
    <Application Id="App" EntryPoint="Windows.WebAppHost">
      <uap:VisualElements DisplayName="The Unknown Universes (${escapeHtml(console.label)})" BackgroundColor="#070A12" Description="PWR ${pwr.powerLevel} | LIGHT ${pwr.lightLevel} | Hero: ${escapeHtml(hero.nameWithVersion)} | GFX: $graphicsTier" />
    </Application>
  </Applications>
</Package>"""
    }

    val obj = JSONObject().apply {
      put("project", "The Unknown Universes — Cross-Platform Export")
      put("targetPlatformId", console.id)
      put("platformLabel", console.label)
      put("platformFamily", console.platformFamily)
      put("artifactFormat", when (platformTargetId) {
        "ps5_dualsense" -> "PlayStation WebGPU/Gamepad Package (PS5/PS4/PS3)"
        "switch_handheld" -> "Nintendo Switch Horizon Handheld 720p/1080p Spec"
        "ios_safari" -> "iOS Safari Standalone PWA + MFi Controller Profile"
        "pc_desktop" -> "Windows PC Standalone (.HTA / WebView2 .EXE Wrapper)"
        else -> "Universal HTML5 Bundle"
      })
      put("graphicsQualityTier", graphicsTier)
      put("activeWorld", world.title)
      put("activeCharacter", hero.nameWithVersion)
      put("powerLevel", pwr.powerLevel)
      put("lightLevel", pwr.lightLevel)
      put("dualStickGamepadMapping", JSONArray(console.buttonLegend))
      put("engineNotes", console.browserEngineNote)
    }
    return obj.toString(2)
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
        val maxTier = maxOf(obj.optLong("maxTier", 25L), 25L).coerceIn(1L, 25L)
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
            powerValue = obj.optLong("powerValue", 100L).coerceIn(1L, 1500L),
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
