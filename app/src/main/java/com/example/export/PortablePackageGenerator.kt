package com.example.export

import com.example.BuildConfig
import com.example.data.model.ArmoryItem
import com.example.data.model.OperatorProfile
import com.example.data.model.RigAsset
import com.example.data.model.UniverseCatalog
import org.json.JSONArray
import org.json.JSONObject

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
   * Generates a portable JSON bundle containing OS state, Armory items, and Rig keyframes.
   */
  fun generatePortableJsonBundle(
    profile: OperatorProfile,
    armory: List<ArmoryItem>,
    rigs: List<RigAsset>,
  ): String {
    val root = JSONObject()
    root.put("project", "The Unknown Universes")
    root.put("packageName", BuildConfig.APPLICATION_ID)
    root.put("osEnvironment", "UnknownOS v2.4")
    root.put("callsign", profile.callsign)
    root.put("activeUniverse", profile.activeUniverseId)
    root.put("activeMode", profile.activeMode)
    root.put("consolePreset", profile.consolePreset)
    root.put("totalEssence", profile.totalEssence)

    val armoryArr = JSONArray()
    armory.forEach { item ->
      armoryArr.put(
        JSONObject().apply {
          put("name", item.name)
          put("category", item.category)
          put("universeTag", item.universeTag)
          put("tier", item.tier)
          put("powerValue", item.powerValue)
          put("cooldownSec", item.cooldownSec)
          put("equipped", item.equipped)
        }
      )
    }
    root.put("armory", armoryArr)

    val rigsArr = JSONArray()
    rigs.forEach { rig ->
      rigsArr.put(
        JSONObject().apply {
          put("name", rig.name)
          put("archetype", rig.archetype)
          put("activeClip", rig.activeClip)
          put("ikEnabled", rig.ikEnabled)
          put("rigScale", rig.rigScale)
          put("playbackSpeed", rig.playbackSpeed)
          put("poseAngles", JSONObject(rig.poseAngles))
          put("keyframeFrames", JSONArray(rig.keyframeFrames))
        }
      )
    }
    val seriesArr = JSONArray()
    com.example.data.model.OriginalVaultCatalog.continuationSeries.forEach { s ->
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
    root.put("vaultArtworksCount", com.example.data.model.OriginalVaultCatalog.artworks.size)
    root.put("vaultVideosCount", com.example.data.model.OriginalVaultCatalog.videos.size)
    root.put("rigs", rigsArr)
    return root.toString(2)
  }

  private fun escapeHtml(raw: String): String =
    raw
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
}
