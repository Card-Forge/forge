<?xml version="1.0" encoding="UTF-8"?>
<tileset version="1.10" tiledversion="1.10.1" name="FarmFood" tilewidth="16" tileheight="16" tilecount="4096" columns="64">
 <image source="FarmFood.png" width="1024" height="1024"/>
 <tile id="964" probability="0.05"/>
 <tile id="1024">
  <objectgroup draworder="index" id="2">
   <object id="1" x="2" y="1" width="14" height="15"/>
  </objectgroup>
 </tile>
 <tile id="1025">
  <objectgroup draworder="index" id="2">
   <object id="1" x="0" y="1" width="16" height="15"/>
  </objectgroup>
 </tile>
 <tile id="1026">
  <objectgroup draworder="index" id="2">
   <object id="1" x="0" y="1" width="14" height="15"/>
  </objectgroup>
 </tile>
 <tile id="1027">
  <objectgroup draworder="index" id="2">
   <object id="1" x="0" y="1" width="16" height="14"/>
  </objectgroup>
 </tile>
 <tile id="1088">
  <objectgroup draworder="index" id="2">
   <object id="1" x="3" y="0" width="10" height="15"/>
  </objectgroup>
 </tile>
 <tile id="1089">
  <objectgroup draworder="index" id="2">
   <object id="1" x="5" y="0" width="7" height="16"/>
  </objectgroup>
 </tile>
 <tile id="1090">
  <objectgroup draworder="index" id="2">
   <object id="1" x="4" y="1" width="9" height="15"/>
  </objectgroup>
 </tile>
 <tile id="1091">
  <objectgroup draworder="index" id="2">
   <object id="1" x="2" y="1" width="14" height="14"/>
  </objectgroup>
 </tile>
 <tile id="1092">
  <objectgroup draworder="index" id="2">
   <object id="1" x="0" y="1" width="14" height="14"/>
  </objectgroup>
 </tile>
 <tile id="1152">
  <objectgroup draworder="index" id="2">
   <object id="1" x="2" y="0" width="14" height="15"/>
  </objectgroup>
 </tile>
 <tile id="1153">
  <objectgroup draworder="index" id="2">
   <object id="1" x="0" y="0" width="16" height="15"/>
  </objectgroup>
 </tile>
 <tile id="1154">
  <objectgroup draworder="index" id="2">
   <object id="1" x="0" y="0" width="14" height="15"/>
  </objectgroup>
 </tile>
 <tile id="1155">
  <objectgroup draworder="index" id="2">
   <object id="1" x="0" y="0" width="16" height="16"/>
  </objectgroup>
 </tile>
 <wangsets>
  <wangset name="Paths" type="mixed" tile="-1">
   <wangcolor name="Dirt Path" color="#ff0000" tile="-1" probability="1"/>
   <wangcolor name="Edge" color="#00ff00" tile="-1" probability="1"/>
   <wangtile tileid="1664" wangid="0,2,0,1,0,2,0,2"/>
   <wangtile tileid="1665" wangid="0,2,0,1,0,1,0,2"/>
   <wangtile tileid="1666" wangid="0,2,0,2,0,1,0,2"/>
   <wangtile tileid="1728" wangid="0,1,0,1,0,2,0,2"/>
   <wangtile tileid="1729" wangid="0,1,0,1,0,1,0,1"/>
   <wangtile tileid="1730" wangid="0,2,0,2,0,1,0,1"/>
   <wangtile tileid="1792" wangid="0,1,0,2,0,2,0,2"/>
   <wangtile tileid="1793" wangid="0,1,0,2,0,2,0,1"/>
   <wangtile tileid="1794" wangid="0,2,0,2,0,2,0,1"/>
   <wangtile tileid="1860" wangid="0,1,0,2,0,1,0,1"/>
   <wangtile tileid="1861" wangid="0,1,0,1,0,2,0,1"/>
   <wangtile tileid="1924" wangid="0,2,0,1,0,1,0,1"/>
   <wangtile tileid="1925" wangid="0,1,0,1,0,1,0,2"/>
  </wangset>
  <wangset name="Walls" type="edge" tile="-1">
   <wangcolor name="Stone Fence" color="#ff0000" tile="-1" probability="1"/>
   <wangcolor name="Wood Fence" color="#00ff00" tile="-1" probability="1"/>
   <wangtile tileid="832" wangid="0,0,2,0,2,0,0,0"/>
   <wangtile tileid="833" wangid="0,0,2,0,2,0,2,0"/>
   <wangtile tileid="834" wangid="0,0,0,0,2,0,2,0"/>
   <wangtile tileid="835" wangid="0,0,2,0,0,0,2,0"/>
   <wangtile tileid="896" wangid="2,0,0,0,0,0,0,0"/>
   <wangtile tileid="897" wangid="2,0,0,0,2,0,0,0"/>
   <wangtile tileid="898" wangid="0,0,0,0,2,0,0,0"/>
   <wangtile tileid="899" wangid="0,0,2,0,0,0,0,0"/>
   <wangtile tileid="900" wangid="0,0,0,0,0,0,2,0"/>
   <wangtile tileid="960" wangid="2,0,2,0,0,0,0,0"/>
   <wangtile tileid="961" wangid="2,0,2,0,0,0,2,0"/>
   <wangtile tileid="962" wangid="2,0,0,0,0,0,2,0"/>
   <wangtile tileid="963" wangid="2,0,2,0,2,0,2,0"/>
   <wangtile tileid="964" wangid="0,0,2,0,0,0,2,0"/>
   <wangtile tileid="1024" wangid="0,0,1,0,1,0,0,0"/>
   <wangtile tileid="1025" wangid="0,0,1,0,1,0,1,0"/>
   <wangtile tileid="1026" wangid="0,0,0,0,1,0,1,0"/>
   <wangtile tileid="1027" wangid="0,0,1,0,0,0,1,0"/>
   <wangtile tileid="1088" wangid="1,0,0,0,0,0,0,0"/>
   <wangtile tileid="1089" wangid="1,0,0,0,1,0,0,0"/>
   <wangtile tileid="1090" wangid="0,0,0,0,1,0,0,0"/>
   <wangtile tileid="1091" wangid="0,0,1,0,0,0,0,0"/>
   <wangtile tileid="1092" wangid="0,0,0,0,0,0,1,0"/>
   <wangtile tileid="1152" wangid="1,0,1,0,0,0,0,0"/>
   <wangtile tileid="1153" wangid="1,0,1,0,0,0,1,0"/>
   <wangtile tileid="1154" wangid="1,0,0,0,0,0,1,0"/>
   <wangtile tileid="1155" wangid="1,0,1,0,1,0,1,0"/>
  </wangset>
  <wangset name="Fields" type="mixed" tile="-1">
   <wangcolor name="" color="#ff0000" tile="-1" probability="1"/>
   <wangcolor name="" color="#00ff00" tile="-1" probability="1"/>
   <wangcolor name="" color="#0000ff" tile="-1" probability="1"/>
   <wangcolor name="" color="#ff7700" tile="-1" probability="1"/>
   <wangcolor name="" color="#00e9ff" tile="-1" probability="1"/>
   <wangcolor name="" color="#ff00d8" tile="-1" probability="1"/>
   <wangcolor name="" color="#ffff00" tile="-1" probability="1"/>
   <wangcolor name="" color="#a000ff" tile="-1" probability="1"/>
   <wangcolor name="" color="#00ffa1" tile="-1" probability="1"/>
   <wangcolor name="" color="#ffa8a8" tile="-1" probability="1"/>
   <wangtile tileid="527" wangid="0,0,6,6,6,0,0,0"/>
   <wangtile tileid="528" wangid="0,0,0,0,6,6,6,0"/>
   <wangtile tileid="529" wangid="0,0,0,5,5,5,0,0"/>
   <wangtile tileid="530" wangid="6,6,6,6,6,0,0,0"/>
   <wangtile tileid="531" wangid="6,0,0,0,6,6,6,6"/>
   <wangtile tileid="532" wangid="5,5,0,5,5,5,0,5"/>
   <wangtile tileid="577" wangid="0,2,0,2,0,0,0,0"/>
   <wangtile tileid="578" wangid="0,2,0,2,0,2,0,2"/>
   <wangtile tileid="579" wangid="0,0,0,0,0,2,0,2"/>
   <wangtile tileid="581" wangid="0,3,0,3,0,0,0,0"/>
   <wangtile tileid="582" wangid="0,3,0,3,0,3,0,3"/>
   <wangtile tileid="583" wangid="0,0,0,0,0,3,0,3"/>
   <wangtile tileid="589" wangid="0,5,5,5,0,0,0,0"/>
   <wangtile tileid="590" wangid="0,0,0,0,0,5,5,5"/>
   <wangtile tileid="591" wangid="6,6,6,0,0,0,0,0"/>
   <wangtile tileid="592" wangid="6,0,0,0,0,0,6,6"/>
   <wangtile tileid="593" wangid="5,5,0,0,0,0,0,5"/>
   <wangtile tileid="594" wangid="6,6,6,0,0,0,6,6"/>
   <wangtile tileid="595" wangid="0,0,6,6,6,6,6,0"/>
   <wangtile tileid="596" wangid="0,5,5,5,0,5,5,5"/>
   <wangtile tileid="641" wangid="0,1,0,1,0,0,0,0"/>
   <wangtile tileid="642" wangid="0,1,0,1,0,1,0,1"/>
   <wangtile tileid="643" wangid="0,0,0,0,0,1,0,1"/>
   <wangtile tileid="645" wangid="0,4,0,4,0,0,0,0"/>
   <wangtile tileid="646" wangid="0,4,0,4,0,4,0,4"/>
   <wangtile tileid="647" wangid="0,0,0,0,0,4,0,4"/>
   <wangtile tileid="660" wangid="6,6,6,6,6,6,6,6"/>
   <wangtile tileid="1681" wangid="8,8,7,7,7,8,8,8"/>
   <wangtile tileid="1682" wangid="8,8,7,7,7,7,7,8"/>
   <wangtile tileid="1683" wangid="8,8,8,8,7,7,7,8"/>
   <wangtile tileid="1745" wangid="7,7,7,7,7,8,8,8"/>
   <wangtile tileid="1746" wangid="7,7,7,7,7,7,7,7"/>
   <wangtile tileid="1747" wangid="7,8,8,8,7,7,7,7"/>
   <wangtile tileid="1809" wangid="7,7,7,8,8,8,8,8"/>
   <wangtile tileid="1810" wangid="7,7,7,8,8,8,7,7"/>
   <wangtile tileid="1811" wangid="7,8,8,8,8,8,7,7"/>
   <wangtile tileid="1873" wangid="8,8,9,9,9,8,8,8"/>
   <wangtile tileid="1874" wangid="8,8,9,9,9,9,9,8"/>
   <wangtile tileid="1875" wangid="8,8,8,8,9,9,9,8"/>
   <wangtile tileid="1937" wangid="9,9,9,9,9,8,8,8"/>
   <wangtile tileid="1938" wangid="9,9,9,9,9,9,9,9"/>
   <wangtile tileid="1939" wangid="9,8,8,8,9,9,9,9"/>
   <wangtile tileid="2001" wangid="9,9,9,8,8,8,8,8"/>
   <wangtile tileid="2002" wangid="9,9,9,8,8,8,9,9"/>
   <wangtile tileid="2003" wangid="9,8,8,8,8,8,9,9"/>
  </wangset>
 </wangsets>
</tileset>
