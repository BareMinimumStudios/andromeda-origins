-- Andromeda Origins Figura compatibility helper
-- Target: Figura 0.1.6 / Minecraft 1.21.1
-- Copy this file into your avatar scripts and require it with:
--   local ao = require("andromeda_origins")

local M = {}

local ORIGIN_TAGS = {
   arachne="arachne", faerie="faerie", fenrkin="fenrkin", gorgon="gorgon",
   human="humanity", lichling="lichling", manticore="manticore", nereid="nereid",
   satyr="satyr", selkie="selkie", siren="siren", veilborn="veilborn", wyverian="wyverian"
}

local STATES = {
   arachne_scurry={origin="arachne",hook=1}, arachne_web_spit={origin="arachne",hook=2}, arachne_weaving={origin="arachne",hook=3}, arachne_leap={origin="arachne",hook=4},
   faerie_flutter={origin="faerie",hook=1}, faerie_illusion={origin="faerie",hook=2}, faerie_air_jump={origin="faerie",hook=3},
   fenrkin_howl={origin="fenrkin",hook=1}, fenrkin_hunt={origin="fenrkin",hook=2}, fenrkin_stamina_surge={origin="fenrkin",hook=3}, fenrkin_underdog={origin="fenrkin",hook=4}, fenrkin_pounce={origin="fenrkin",hook=5},
   gorgon_gaze={origin="gorgon",hook=1}, gorgon_transference={origin="gorgon",hook=2},
   human_indomitable={origin="humanity",hook=1}, humanity_indomitable={origin="humanity",hook=1},
   human_mortal_resolve={origin="humanity",resource="andromeda_origins:humanity/figura_mortal_resolve",tags={"andromeda_mortal_resolve","andromeda_champion_mortal_resolve"}}, humanity_mortal_resolve={origin="humanity",resource="andromeda_origins:humanity/figura_mortal_resolve",tags={"andromeda_mortal_resolve","andromeda_champion_mortal_resolve"}}, human_champion_mortal_resolve={origin="humanity",resource="andromeda_origins:humanity/figura_mortal_resolve",tags={"andromeda_champion_mortal_resolve"}},
   lichling_self_defiance={origin="lichling",hook=1}, lichling_target_defiance={origin="lichling",hook=2}, lichling_chimes={origin="lichling",hook=3},
   manticore_lunge={origin="manticore",hook=1}, manticore_beast_of_blood={origin="manticore",hook=2}, manticore_wet_shake={origin="manticore",hook=3},
   nereid_aura_channel={origin="nereid",hook=1}, nereid_aura_finish={origin="nereid",hook=2}, nereid_submersion={origin="nereid",hook=3},
   satyr_max_momentum={origin="satyr",hook=1}, satyr_swift_leap={origin="satyr",hook=2}, satyr_stomp={origin="satyr",hook=3}, satyr_rush={origin="satyr",hook=4}, satyr_vigil={origin="satyr",hook=5},
   selkie_coastal_phalanx_cast={origin="selkie",hook=1}, selkie_coastal_phalanx_hit={origin="selkie",hook=2}, selkie_surging_tides={origin="selkie",hook=3}, selkie_sealskin_bastion={origin="selkie",hook=4},
   siren_infatuation_cast={origin="siren",hook=1}, siren_infatuation_hit={origin="siren",hook=2}, siren_wail={origin="siren",hook=3},
   veilborn_transposition={origin="veilborn",hook=1}, veilborn_auroral_mirage={origin="veilborn",hook=2}, veilborn_wet_shake={origin="veilborn",hook=3},
   wyverian_fire_breath={origin="wyverian",hook=1}, wyverian_fireball_charge={origin="wyverian",hook=2}, wyverian_fireball_release={origin="wyverian",hook=3}, wyverian_wing_flap={origin="wyverian",hook=4}, wyverian_hover={origin="wyverian",hook=5}
}

local function scalar(v)
   local t=type(v)
   if t=="number" then return v end
   if t=="boolean" then return v and 1 or 0 end
   if t~="table" then return nil end
   for _,key in ipairs({"data","Data","value","Value"}) do local n=scalar(v[key]); if n~=nil then return n end end
   for _,child in pairs(v) do if type(child)=="number" or type(child)=="boolean" then local n=scalar(child); if n~=nil then return n end end end
   return nil
end

local function findPower(node,id,seen)
   if type(node)~="table" then return nil,false end
   seen=seen or {}; if seen[node] then return nil,false end; seen[node]=true
   if node[id]~=nil then return scalar(node[id]),true end
   local entryId=node.id or node.Id or node.Type or node.type
   if entryId==id then return scalar(node.data or node.Data or node.value or node.Value or node),true end
   for _,child in pairs(node) do if type(child)=="table" then local n,found=findPower(child,id,seen); if found then return n,true end end end
   return nil,false
end

local function tagsFrom(nbt)
   local result={}; if type(nbt)~="table" then return result end
   local tags=nbt.Tags or nbt.tags; if type(tags)~="table" then return result end
   for _,tag in pairs(tags) do if type(tag)=="string" then result[tag]=true end end
   return result
end

function M.snapshot() return player:getNbt() end
function M.hasTag(tag,nbt) nbt=nbt or M.snapshot(); return tagsFrom(nbt)[tag]==true end
function M.hasPower(id,nbt) nbt=nbt or M.snapshot(); local _,found=findPower(nbt,id); return found end
function M.resource(id,nbt) nbt=nbt or M.snapshot(); local n,found=findPower(nbt,id); if not found then return nil end; return n==nil and 1 or n end
function M.hook(index,nbt) local value=M.resource("andromeda_origins:common/figura_"..tostring(index),nbt); return value~=nil and value>0 end
function M.getOrigin(nbt)
   nbt=nbt or M.snapshot(); local tags=tagsFrom(nbt)
   for tag,origin in pairs(ORIGIN_TAGS) do if tags[tag] then return origin end end
   for _,origin in pairs(ORIGIN_TAGS) do
      if M.hasPower("andromeda_origins:"..origin.."/figura",nbt) then return origin end
   end
   return nil
end
function M.isChampion(nbt) nbt=nbt or M.snapshot(); return M.hasPower("andromeda_origins:common/champion",nbt) end
function M.state(name,nbt)
   nbt=nbt or M.snapshot(); local def=STATES[name]; if def==nil then return false end
   if def.origin~=nil and M.getOrigin(nbt)~=def.origin then return false end
   if def.hook~=nil then return M.hook(def.hook,nbt) end
   if def.resource~=nil then local value=M.resource(def.resource,nbt); if value~=nil and value>0 then return true end end
   if def.tags~=nil then for _,tag in ipairs(def.tags) do if M.hasTag(tag,nbt) then return true end end end
   return false
end
function M.extraBoneLoaded()
   if client==nil or client.isModLoaded==nil then return false end
   local ok,loaded=pcall(function() return client:isModLoaded("figuraextrabone") end)
   if ok then return loaded==true end
   ok,loaded=pcall(function() return client.isModLoaded("figuraextrabone") end)
   return ok and loaded==true
end
function M.states() return STATES end
return M
