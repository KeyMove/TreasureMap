/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.github.KeyMove;

import java.io.File;
import java.io.IOException;
import static java.lang.System.out;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapFont;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.EulerAngle;

/**
 *
 * @author Administrator
 */
public class TreasureMap extends JavaPlugin implements Listener{
    
    YamlConfiguration 配置文件;
    YamlConfiguration 宝藏物品;
    YamlConfiguration 宝图缓存文件;
    File 宝图缓存文件信息=new File(getDataFolder(),"MapID.dat");
    寻找藏宝地点 寻找器=new 寻找藏宝地点();
    double 掉落几率=0.5;
    String 获取掉落1="玩家@p杀死了一只@e,掉落了一张藏宝图";
    String 获取掉落2="玩家@p杀死了一只生物掉落了一张藏宝图";
    String 获取信息="玩家@p找到了@i";
    String 打开宝箱信息="玩家@p打开了@i获得了@e";
    String 窗口名称1="§6§l奖励物品列表";
    String 窗口名称2="§6§l宝箱类型列表";
    String 窗口名称3="§6§l选择要修改的宝箱类型";
    String 藏宝图名称="§6§l藏宝图";
    List<Short> 宝图缓存=new ArrayList<>();
    List<Rect> 缓存区域=new ArrayList<>();
    List<Location> 缓存宝藏点=new ArrayList<>();
    List<ItemStack> 宝箱类型列表=new ArrayList<>();
    List<ItemStack> 奖励物品列表=new ArrayList<>();
    List<EntityType> 掉落怪物类型=new ArrayList<>();
    Map<ItemStack,List<ItemStack>> 宝箱类型奖励列表=new HashMap<>();
    Map<String,ItemStack> 玩家编辑宝箱=new HashMap<>();
    List<ItemStack> 空宝箱=new ArrayList<>();
    ItemStack 藏宝图物品=new ItemStack(Material.MAP);
    int 缓存数量=10;
    long 宝箱失效时间=604800000;//毫秒
    ItemStack 盔甲头=new ItemStack(Material.CHEST);
    Inventory 奖励栏=getServer().createInventory(null, 54, "§6§l奖励物品列表");
    Inventory 宝箱类型栏=getServer().createInventory(null, 54, "§6§l宝箱类型列表");
    Field 盔甲架数据;
    Method 盔甲架方法;
    
    Plugin 插件;
    
    public boolean 盔甲架初始化(){
        String ver=getServer().getClass().getPackage().getName();
        ver=ver.substring(ver.lastIndexOf('.')+1)+".";  
        try {
           盔甲架方法=Class.forName("org.bukkit.craftbukkit."+ver+"entity.CraftArmorStand").getDeclaredMethod("getHandle");
           盔甲架数据=Class.forName("net.minecraft.server."+ver+"EntityArmorStand").getDeclaredField("bg");
           盔甲架数据.setAccessible(true);
           return true;
        } catch (ClassNotFoundException | NoSuchFieldException | SecurityException | NoSuchMethodException ex) {
            Logger.getLogger(TreasureMap.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }
    public void 设置盔甲架无法交互(ArmorStand 盔甲架){
        try {
            盔甲架数据.set(盔甲架方法.invoke(盔甲架, (Object[]) null), 31);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(TreasureMap.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public class Rect{
        int x;
        int y;
        int ex;
        int ey;
        Location px;
        public Rect(Location p){
            Location p2=p.clone();
            p.setX(p.getX()-400);
            p.setZ(p.getZ()-400);
            p2.setX(p2.getX()+400);
            p2.setZ(p2.getZ()+400);
            px=p.clone();
            x=Math.abs(p.getBlockX()-p2.getBlockX())/2;
            if(p.getBlockX()>p2.getBlockX())
            {
                x+=p2.getBlockX();
                px.setX(p2.getBlockX());
            }
            else
            {
                x+=p.getBlockX();
                px.setX(p.getBlockX());
            }
            y=Math.abs(p.getBlockZ()-p2.getBlockZ())/2;
            if(p.getBlockZ()>p2.getBlockZ())
            {
                y+=p2.getBlockZ();
                px.setZ(p2.getBlockZ());
            }
            else
            {
                y+=p.getBlockZ();
                px.setZ(p.getBlockZ());
            }
            ex=Math.abs(p.getBlockX()-p2.getBlockX());
            ey=Math.abs(p.getBlockZ()-p2.getBlockZ());
            out.print(px);
            
            out.print(x);
            out.print(y);
            
            out.print(ex);
            out.print(ey);
        }
        public Rect(Location p,Location p2) {
            px=p.clone();
            x=Math.abs(p.getBlockX()-p2.getBlockX())/2;
            if(p.getBlockX()>p2.getBlockX())
            {
                x+=p2.getBlockX();
                px.setX(p2.getBlockX());
            }
            else
            {
                x+=p.getBlockX();
                px.setX(p.getBlockX());
            }
            y=Math.abs(p.getBlockZ()-p2.getBlockZ())/2;
            if(p.getBlockZ()>p2.getBlockZ())
            {
                y+=p2.getBlockZ();
                px.setZ(p2.getBlockZ());
            }
            else
            {
                y+=p.getBlockZ();
                px.setZ(p.getBlockZ());
            }
            ex=Math.abs(p.getBlockX()-p2.getBlockX());
            ey=Math.abs(p.getBlockZ()-p2.getBlockZ());
            out.print(px);
            
            out.print(x);
            out.print(y);
            
            out.print(ex);
            out.print(ey);
        }
        public Location getRandLocation(){
            Location p=px.clone();
            double ax=Math.random()*ex;
            double ay=Math.random()*ey;
            p.setX(p.getX()+ax);
            p.setZ(p.getZ()+ay);
            return p;
        }
        @Override
        public String toString() {
            return "x:"+x+",y:"+y+",ex:"+ex+",ey:"+ey+"loc:"+px;
        }
    }
    public Object 列表中获取随机项(List 列表){
        if(列表.isEmpty())
            return null;
        int index=(int) (列表.size()*Math.random()-0.1);
        //out.print("表大小:"+列表.size()+"值:"+index);
        return 列表.get(index);
    }
    public Inventory 填充窗口(Inventory 窗口,List<ItemStack>... 物品列表){
        窗口.clear();
        int index=0;
        for(List<ItemStack> 物品表:物品列表){
            for(int i=0;i<物品表.size();i++){
                窗口.setItem(index+i, 物品表.get(i));
            }
            index=物品表.size();
        }
        return 窗口;
    }
    public void 配置文件设置默认(File 配置){
            this.saveDefaultConfig();
            掉落几率=0.5;
            缓存数量=10;
            宝箱失效时间=604800000;
            获取掉落1="玩家@p杀死了一只@e,掉落了一张藏宝图";
            获取掉落2="玩家@p杀死了一只生物掉落了一张藏宝图";
            获取信息="玩家@p找到了@i";
            打开宝箱信息="玩家@p打开了@i获得了@e";
            
            配置文件=YamlConfiguration.loadConfiguration(配置);
            World 默认世界=getServer().getWorld("world");
            if(默认世界==null)
                getServer().getWorlds().get(0);
            if(默认世界==null)
            {
                onDisable();
                return;
            }
            Location 出生点=默认世界.getSpawnLocation();
            out.print("默认世界为：");
            out.print(默认世界);
            配置文件.set("Message.1", 获取掉落1);
            配置文件.set("Message.2", 获取掉落2);
            配置文件.set("Message.3", 获取信息);
            配置文件.set("Message.4", 打开宝箱信息);
            配置文件.set("Location.Count", 1);
            配置文件.set("Location.0.sx", 出生点.getBlockX()-400);
            配置文件.set("Location.0.sz", 出生点.getBlockZ()-400);
            配置文件.set("Location.0.ex", 出生点.getBlockX()+400);
            配置文件.set("Location.0.ez", 出生点.getBlockZ()+400);
            配置文件.set("Location.0.world", 默认世界.getName());
            配置文件.set("Map.drop", 0.1);
            配置文件.set("Map.cache", 10);
            配置文件.set("Map.TimeOut", 7);
            Location 出生点对称=出生点.clone();
            出生点.setX(出生点.getX()-400);
            出生点.setY(出生点.getY()-400);
            出生点对称.setX(出生点对称.getX()+400);
            出生点对称.setY(出生点对称.getY()+400);
            缓存区域.add(new Rect(出生点,出生点对称));
            try {
                配置文件.save(配置);
            } catch (IOException ex) {
                Logger.getLogger(TreasureMap.class.getName()).log(Level.SEVERE, null, ex);
            }
    }
    public void 保存物品列表(List 列表,String 名称){
        宝藏物品.set(名称+".Item", 奖励物品列表);
    }
    public void 保存设置参数(){
        配置文件.set("Location.Count", 缓存区域.size());
        for(int i=0;i<缓存区域.size();i++){
            Rect 区域=缓存区域.get(i);
            配置文件.set("Location."+i+".sx", 区域.x);
            配置文件.set("Location."+i+".sz", 区域.y);
            配置文件.set("Location."+i+".ex", 区域.ex);
            配置文件.set("Location."+i+".ez", 区域.ex);
            配置文件.set("Location."+i+".world", 区域.px.getWorld().getName());
        }
    }
    public void 保存宝藏物品(){
        宝藏物品.set("MapItem", 藏宝图物品);
        宝藏物品.set("ChestType", 宝箱类型列表);
        宝藏物品.set("NullChest", 空宝箱);
        for(int i=0;i<宝箱类型列表.size();i++){
             宝藏物品.set("ChestItem."+i,(List<ItemStack>)宝箱类型奖励列表.get(宝箱类型列表.get(i)));
        }
        try {
            宝藏物品.save(new File(getDataFolder(),"TreasureItem.dat"));
        } catch (IOException ex) {
            Logger.getLogger(TreasureMap.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public void 默认奖励道具(){
        奖励物品列表.add(new ItemStack(Material.DIAMOND_BLOCK));
        奖励物品列表.add(new ItemStack(Material.IRON_BLOCK));
        奖励物品列表.add(new ItemStack(Material.GOLD_BLOCK));
        List<ItemStack> 默认物品列表=new ArrayList<>();
        默认物品列表.add(new ItemStack(Material.BEACON));
        默认物品列表.add(new ItemStack(Material.DIAMOND_PICKAXE));
        默认物品列表.add(new ItemStack(Material.DIAMOND_BLOCK));
        List<ItemStack> 默认物品列表2=new ArrayList<>();
        默认物品列表2.add(new ItemStack(Material.DIAMOND_BLOCK));
        默认物品列表2.add(new ItemStack(Material.IRON_BLOCK));
        默认物品列表2.add(new ItemStack(Material.GOLD_BLOCK));
        宝箱类型列表.add(修改物品数据(new ItemStack(Material.CHEST), "§6§l宝藏箱", "§a§l沉甸甸的宝藏箱子"));
        宝箱类型列表.add(修改物品数据(new ItemStack(Material.ENDER_CHEST), "§6§l华丽宝藏箱", "§a§l里面会不会装着好东西呢"));
        藏宝图物品=修改物品数据(new ItemStack(Material.MAP),藏宝图名称,"§a这是一张藏宝图");
        宝箱类型奖励列表.put(宝箱类型列表.get(0), 默认物品列表2);       
        宝箱类型奖励列表.put(宝箱类型列表.get(1), 默认物品列表);
    }
    public void 加载奖励列表文件(){
        File 配置=new File(getDataFolder(),"TreasureItem.dat");
        if(!配置.exists()){
            try {
                配置.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(TreasureMap.class.getName()).log(Level.SEVERE, null, ex);
            }
            宝藏物品=YamlConfiguration.loadConfiguration(配置);
            默认奖励道具();
            保存宝藏物品();
        }
        else{
            宝藏物品=YamlConfiguration.loadConfiguration(配置);
            宝箱类型列表=(List<ItemStack>)宝藏物品.getList("ChestType");
            if(宝箱类型列表.isEmpty()){
                默认奖励道具();
                return;
            }
            for(int i=0;i<宝箱类型列表.size();i++){
                宝箱类型奖励列表.put(宝箱类型列表.get(i), (List<ItemStack>)宝藏物品.getList("ChestItem."+i));
            }
            if(宝藏物品.isList("NullChest"))
                空宝箱=(List<ItemStack>)宝藏物品.getList("NullChest");
            藏宝图物品=宝藏物品.getItemStack("MapItem");
            藏宝图名称=藏宝图物品.getItemMeta().getDisplayName();
        }
    }
    public void 加载配置文件(){
        File 配置=new File(getDataFolder(),"config.yml");
        if(!配置.exists())
        {
            配置文件设置默认(配置);
        }else{
            配置文件=YamlConfiguration.loadConfiguration(配置);
            boolean error=false;
            获取掉落1=(String) 配置文件.get("Message.1");
            if(获取掉落1==null)error=true;
            获取掉落2=(String) 配置文件.get("Message.2");
            if(获取掉落2==null)error=true;
            获取信息=(String) 配置文件.get("Message.3");
            if(获取信息==null)error=true;
            打开宝箱信息=(String) 配置文件.get("Message.4");
            if(打开宝箱信息==null)error=true;
            out.print(error);
            if(!配置文件.isInt("Location.Count"))error=true;
            if(!error){
            for(int i=0;i<配置文件.getInt("Location.Count");i++){
                String 世界名称=(String) 配置文件.get("Location."+i+".world");
                if(世界名称==null){
                    if(i==1)
                        error=true;
                    break;
                }
                World 世界=getServer().getWorld(世界名称);
                if(世界==null){
                    if(i==1)
                        error=true;
                    break;
                }
                out.print(世界);
                Location 点=世界.getSpawnLocation().clone();
                点.setX((int)配置文件.get("Location."+i+".sx"));
                点.setZ((int)配置文件.get("Location."+i+".sz"));
                Location 第二点=点.clone();
                第二点.setX((int)配置文件.get("Location."+i+".ex"));
                第二点.setZ((int)配置文件.get("Location."+i+".ez"));
                out.print(点);
                缓存区域.add(new Rect(点,第二点));
                out.print(缓存区域);
            }
            掉落几率=配置文件.getDouble("Map.drop");
            缓存数量=配置文件.getInt("Map.cache");
            宝箱失效时间=配置文件.getLong("Map.TimeOut");
            宝箱失效时间*=86400000;
            }
            out.print(error);
            if(error)
            {
                配置文件设置默认(配置);
            }
        }
        if(!宝图缓存文件信息.exists()){
            try {
                宝图缓存文件信息.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(TreasureMap.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        宝图缓存文件=YamlConfiguration.loadConfiguration(宝图缓存文件信息);
        if(宝图缓存文件.isList("MapID")){
            for(Integer id:(List<Integer>)宝图缓存文件.getList("MapID"))
            {
                设置藏宝图((short)id.intValue());
            }
        }
    }
    
    @Override
    public void onEnable() {
        插件=this;
        if(!盔甲架初始化())
        {
            out.print("初始化失败");
            onDisable();
            return;
        }
        奖励物品列表.clear();
        宝箱类型列表.clear();
        缓存区域.clear();
        缓存宝藏点.clear();
        加载配置文件();
        加载奖励列表文件();
        out.print("藏宝图插件加载成功!");
        out.print("开始寻找藏宝地点!");
        寻找器.开始寻找();
        getServer().getPluginManager().registerEvents(this, this);
    }
    public void 创建占位盔甲架(Location 点){
        if(!点.getChunk().isLoaded())
        {
            点.getChunk().load(true);
        }
        ArmorStand 盔甲架=(ArmorStand)点.getWorld().spawnEntity(点, EntityType.ARMOR_STAND);
        out.print(盔甲架.toString());
        盔甲架.setSmall(true);
        盔甲架.setGravity(false);
        盔甲架.setVisible(false);
        设置盔甲架无法交互(盔甲架);
        盔甲架.setHelmet(((ItemStack)列表中获取随机项(宝箱类型列表)).clone());
        盔甲架.setBoots(修改物品数据(new ItemStack(Material.COMPASS), String.valueOf(System.currentTimeMillis())));
        double X=((Math.random()*140)-70)*Math.PI/180;
        double Y=((Math.random()*140)-70)*Math.PI/180;
        Y=Math.abs(Y);
        盔甲架.setHeadPose(盔甲架.getHeadPose().setX(X));
        盔甲架.setHeadPose(盔甲架.getHeadPose().setY(Y));
        out.print("X:"+X+"Y:"+Y);
    }
    public class 寻找藏宝地点 implements Runnable{
        boolean isRun=false;
        Thread 线程;
        public void 开始寻找(){
            if(isRun)
            {
                out.print("正在运行");
            }else{
                线程=new Thread(this);
                线程.start();
            isRun=true;
            }
        }
        @Override
        public void run() {
            if(缓存区域.isEmpty())
            {
                out.print("缓存空");
                isRun=false;
                return;
            }
            out.print("开始寻找!");
            while(缓存宝藏点.size()<缓存数量){
                getServer().getScheduler().scheduleSyncDelayedTask(插件,new Runnable() {

                    @Override
                    public void run() {
                         Rect 区域=缓存区域.get((int) ((Math.random()*缓存区域.size())-0.1));
                Location 点=区域.getRandLocation();
                Chunk 区块=点.getChunk();
                if(!区块.isLoaded())
                {
                    out.print("未加载");
                    if(!区块.load(true))
                    {
                        out.print("加载失败");
                        return;
                    }
                    out.print("加载成功");
                }
                World 世界=点.getWorld();
                int i=190;
                int other=0;
                while(i>1){
                    点.setY(i);
                    Material 类型=世界.getBlockAt(点).getType();
                    switch(类型){
                        case AIR:
                        case WATER:
                            break;
                        case LONG_GRASS:
                        case SNOW:
                        case RED_ROSE:
                        case YELLOW_FLOWER:
                        case BROWN_MUSHROOM:
                        case RED_MUSHROOM:
                            other++;
                            break;
                        case GRASS:
                        case STONE:
                        case SAND:
                        case DIRT:
                        case STAINED_CLAY:
                            缓存宝藏点.add(点.clone());
                            out.print(点);
                            i=1;
                            break;
                        default:
                            i=1;
                            break;
                    }
                    i--;
                }
                    }
                });
               
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ex) {
                    Logger.getLogger(TreasureMap.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            isRun=false;
            out.print("寻找完成");
        }
    }
    
    ItemStack 修改物品数据(ItemStack 物品,String 名称,String... 标签){
        ItemMeta 物品属性=物品.getItemMeta();
        物品属性.setDisplayName(名称.replaceAll("&", "§"));
        List<String> 标签列表=new ArrayList<>();
        for(String s:标签)
            标签列表.add(s.replaceAll("&", "§"));
        if(!标签列表.isEmpty())
            物品属性.setLore(标签列表);
        物品.setItemMeta(物品属性);
        return 物品;
    }
    void 是否销毁藏宝图(ItemStack 物品){
        if(物品.hasItemMeta())
            if(物品.getItemMeta().hasDisplayName())
                if(物品.getItemMeta().getDisplayName().equalsIgnoreCase(藏宝图名称))
                {
                    out.print("清除缓存");
                    宝图缓存.remove(物品.getDurability());
                    宝图储存();
                }
    }
    void 给玩家物品(Player 玩家,ItemStack 物品){
        if(玩家.getInventory().firstEmpty()!=-1)
        {
                玩家.getInventory().addItem(物品);
                玩家.updateInventory();
                out.print("奖励玩家");
            }
            else{
                玩家.getWorld().dropItem(玩家.getLocation(), 物品);
                out.print("掉落玩家");
            }
    }
    @EventHandler
    public void 实体死亡事件(EntityDeathEvent 事件){
        Entity 死亡实体=事件.getEntity();
        if(事件.getEntityType()==EntityType.PLAYER)
            return;
        if(!掉落怪物类型.isEmpty())
            if(掉落怪物类型.indexOf(事件.getEntityType())==-1)
                return;
        Player 玩家=事件.getEntity().getKiller();
        if(玩家==null)
            return;
        if(Math.random()>掉落几率)
            return;
        if(!缓存宝藏点.isEmpty())
        {
            for(Location 点:缓存宝藏点)
            {
                out.print(点);
                创建占位盔甲架(点);                
                事件.getDrops().add(创建藏宝图(点, 藏宝图物品.clone()));
                缓存宝藏点.remove(点);
                寻找器.开始寻找();
                break;
            }
        }else{
            out.print("缓存空");
            return;
        }
        String 信息;
        if(死亡实体.getCustomName()!=null){
            信息=获取掉落1.replaceAll("@p", 玩家.getName()).replaceAll("@e", 死亡实体.getCustomName()).replaceAll("&", "§");
            getServer().broadcastMessage(信息);
        }else{
            信息=获取掉落2.replaceAll("@p", 玩家.getName()).replaceAll("&", "§");
            getServer().broadcastMessage(信息);
        }
        宝图储存();
    }
    @EventHandler void 玩家打开宝箱(PlayerInteractEvent 事件){
        ItemStack 物品=事件.getItem();
        Player 玩家=事件.getPlayer();
        if(物品==null)return;
        物品=物品.clone();
        物品.setAmount(1);
        if(宝箱类型列表.indexOf(物品)==-1)return;
        ItemStack 奖励物品=(ItemStack) 列表中获取随机项(宝箱类型奖励列表.get(物品));
        事件.setCancelled(true);
        if(奖励物品==null){
            getServer().broadcastMessage("奖品池空");
            return;
        }
        String 奖励物品名称=奖励物品.getType().toString();
        String 物品名称=物品.getType().toString();
        if(奖励物品.hasItemMeta())
            if(奖励物品.getItemMeta().hasDisplayName())
                奖励物品名称=奖励物品.getItemMeta().getDisplayName();
        if(物品.hasItemMeta())
            if(物品.getItemMeta().hasDisplayName())
                物品名称=物品.getItemMeta().getDisplayName();
        getServer().broadcastMessage(打开宝箱信息.replaceAll("@p", 玩家.getName()).replaceAll("@i", 物品名称).replaceAll("@e", 奖励物品名称).replaceAll("&", "§"));
        if(物品.getAmount()<=1){
            玩家.setItemInHand(奖励物品);
        }else{
            物品.setAmount(物品.getAmount()-1);
            if(玩家.getInventory().firstEmpty()!=-1)
            {
                玩家.getInventory().addItem(奖励物品.clone());
                
                玩家.updateInventory();
                out.print("奖励玩家");
            }
            else{
                玩家.getWorld().dropItem(玩家.getLocation(), 奖励物品.clone());
                out.print("掉落玩家");
            }
                
        }
    }
    @EventHandler void 玩家右键方块(PlayerInteractEvent 事件){
        ItemStack 物品=事件.getItem();
        if(事件.getAction()!=Action.RIGHT_CLICK_BLOCK)return;
        if(物品==null)return;
        if(物品.getType()!=Material.STICK)return;
        事件.getPlayer().sendMessage(事件.getClickedBlock().toString());
    }
    @EventHandler void 玩家选择宝箱(InventoryClickEvent 窗口){
        if(!窗口.getView().getTitle().equalsIgnoreCase(窗口名称3))return;
        Player 玩家=(Player) 窗口.getWhoClicked();
        窗口.setCancelled(true);
        ItemStack 点击的物品=窗口.getView().getItem(窗口.getRawSlot());
        out.print(点击的物品);
        if(窗口.getRawSlot()<(宝箱类型列表.size()+空宝箱.size())){
            玩家.closeInventory();
            玩家编辑宝箱.put(玩家.getName(), 点击的物品);
            if(空宝箱.indexOf(点击的物品)!=-1){
                奖励栏.clear();
                玩家.openInventory(奖励栏);
                return;
            }
            玩家.openInventory(填充窗口(奖励栏,宝箱类型奖励列表.get(点击的物品)));
        }
    }
    @EventHandler void 玩家编辑完成(InventoryCloseEvent 窗口){
        Inventory 物品栏=窗口.getView().getTopInventory();
        Player 玩家=(Player) 窗口.getPlayer();
        if(!(物品栏.getTitle().equalsIgnoreCase(窗口名称1)||物品栏.getTitle().equalsIgnoreCase(窗口名称2)))return;
        out.print("完成编辑");
        //空宝箱
        if(物品栏.getTitle().equalsIgnoreCase(窗口名称1)){//编辑宝箱奖励物品列表
            奖励物品列表.clear();
            ItemStack 宝箱类型=玩家编辑宝箱.get(玩家.getName());
            int id=宝箱类型列表.indexOf(宝箱类型);
            if(id!=-1){//是否空宝箱
                List<ItemStack> 物品列表=宝箱类型奖励列表.get(宝箱类型列表.get(id));
                物品列表.clear();
                for(ItemStack 物品:物品栏.getContents())
                {
                    if(物品!=null)
                    物品列表.add(物品);
                }
                if(物品列表.isEmpty()){
                    宝箱类型列表.remove(宝箱类型);
                    空宝箱.add(宝箱类型);
                    宝箱类型奖励列表.remove(宝箱类型);
                }
                out.print("更改列表");
            }
            else{
                if(空宝箱.indexOf(宝箱类型)!=-1){
                    List<ItemStack> 物品列表=new ArrayList<>();
                    for(ItemStack 物品:物品栏.getContents())
                    {
                        if(物品!=null)
                        物品列表.add(物品);
                    }
                    if(!物品列表.isEmpty()){
                        空宝箱.remove(宝箱类型);
                        宝箱类型列表.add(宝箱类型);
                        宝箱类型奖励列表.put(宝箱类型, 物品列表);
                    }
                    out.print("添加列表");
                }
            }
        }
        else{//修改宝箱列表
            宝箱类型列表.clear();
            for(ItemStack 物品:物品栏.getContents())
            {
                if(物品!=null)
                {
                    if(宝箱类型奖励列表.containsKey(物品)){
                        宝箱类型列表.add(物品);
                    }
                    else{
                        空宝箱.add(物品);
                    }
                }
            }
        }
    }
    @EventHandler
    public void 玩家交互生物事件(PlayerInteractAtEntityEvent 事件){
        Player 玩家=事件.getPlayer();
        out.print(事件.getRightClicked());
        if(事件.getRightClicked().getType()!=EntityType.ARMOR_STAND)return;
        ArmorStand 盔甲架=(ArmorStand)事件.getRightClicked();
        if(盔甲架.getBoots()==null)return;
        if(盔甲架.getBoots().getType()!=Material.COMPASS)return;
        ItemStack 物品=事件.getPlayer().getItemInHand();
        int x=盔甲架.getLocation().getBlockX();
        int y=盔甲架.getLocation().getBlockZ();
        long time=Long.parseLong(盔甲架.getBoots().getItemMeta().getDisplayName());
        out.print(System.currentTimeMillis()-time);
        out.print(宝箱失效时间);
        if((System.currentTimeMillis()-time)>宝箱失效时间){
            out.print("超时的");
            for(Short i:宝图缓存){
                MapView 地图=getServer().getMap(i);
                if(地图.getCenterX()==x&&地图.getCenterZ()==y)
                {
                    宝图缓存.remove((Short)i);
                    break;
                }
            }
            ItemStack 箱子=盔甲架.getHelmet().clone();
            盔甲架.remove();
            给玩家物品(玩家,箱子);
            寻找器.开始寻找();
            宝图储存();
        }
        else{
        if(物品==null)return;
        if(!物品.hasItemMeta())return;
        if(!物品.getItemMeta().hasDisplayName())return;
        if(!物品.getItemMeta().getDisplayName().equalsIgnoreCase(藏宝图名称))return;
        MapView 地图=getServer().getMap(物品.getDurability());
        if(x!=地图.getCenterX())return;
        if(y!=地图.getCenterZ())return;
        宝图缓存.remove((Short)物品.getDurability());
        ItemStack 箱子=盔甲架.getHelmet().clone();
        盔甲架.remove();
        玩家.setItemInHand(箱子);
        String 箱子名称=箱子.getType().toString();
        if(箱子.hasItemMeta())
            if(箱子.getItemMeta().hasDisplayName())
                箱子名称=箱子.getItemMeta().getDisplayName();
        String 信息=获取信息.replaceAll("@p", 玩家.getName()).replaceAll("@i", 箱子名称).replaceAll("&", "§");
        getServer().broadcastMessage(信息);
        寻找器.开始寻找();
        宝图储存();
        }
    }
    void 宝图储存(){
        try {
            宝图缓存文件.set("MapID", 宝图缓存);
            宝图缓存文件.save(宝图缓存文件信息);
        } catch (IOException ex) {
            Logger.getLogger(TreasureMap.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public class MyMap extends MapRenderer{
        @Override
        public void render(MapView mv, MapCanvas mc, Player player) {
            MapCursorCollection mapcos=new MapCursorCollection();
            mapcos.addCursor(0, 0, (byte)0);
            MapCursor mapcor=mapcos.getCursor(0);
            mapcor.setType(MapCursor.Type.WHITE_CROSS);
            mapcos.addCursor(mapcor);
            mc.setCursors(mapcos);
        }
    }
    ItemStack 创建藏宝图(Location 点,ItemStack 物品){
        MapView 地图=getServer().createMap(点.getWorld());
        宝图缓存.add(地图.getId());
        物品.setDurability(地图.getId());
        MapMeta 地图属性=(MapMeta)物品.getItemMeta();
        地图属性.setScaling(true);
        物品.setItemMeta(地图属性);
        地图.setCenterX(点.getBlockX());
        地图.setCenterZ(点.getBlockZ());
        地图.getRenderers().clear();
        地图.addRenderer(new MyMap());
        地图.setScale(MapView.Scale.CLOSE);
        return 物品;
    }
    void 设置藏宝图(short id){
        MapView 地图=getServer().getMap(id);
        地图.getRenderers().clear();
        地图.addRenderer(new MyMap());
        地图.setScale(MapView.Scale.CLOSE);
    }
    @Override
   public boolean onCommand(CommandSender 命令发送者, Command 命令, String label, String[] 参数列表) {
        if(!命令发送者.hasPermission("op")||!(命令发送者 instanceof Player)){
            return false;
        }
        Player 玩家=(Player)命令发送者;
        if(参数列表.length==0)
        {
            玩家.sendMessage("/map world 设置点");
            玩家.sendMessage("/map item 设置箱子内物品");
            玩家.sendMessage("/map chest 设置宝藏箱子");
            玩家.sendMessage("/map settime <Name> <Lore...> 设置物品信息");
            return false;
        }
        switch(参数列表[0]){
            case "world":
                Location 出生点=玩家.getLocation();
                Location 出生点范围=出生点.clone();
                出生点.setX(出生点.getX()-100);
                出生点.setZ(出生点.getZ()-100);
                出生点范围.setX(出生点范围.getX()+100);
                出生点范围.setZ(出生点范围.getZ()+100);
                缓存区域.add(new Rect(出生点, 出生点范围));
                寻找器.开始寻找();
                break;
            case "item":
                Inventory 宝箱=getServer().createInventory(null, ((宝箱类型列表.size()+空宝箱.size())/9+1)*9, 窗口名称3);
                玩家.openInventory(填充窗口(宝箱,宝箱类型列表,空宝箱));
                break;
            case "chest":
                玩家.openInventory(填充窗口(宝箱类型栏,宝箱类型列表,空宝箱));
                break;
            case "setitem":
                if(参数列表.length==1)
                {
                    玩家.sendMessage("/map setime <Name> <Lore>");
                    return false;
                }
                String[] Lore=null;
                if((参数列表.length-2)!=0){
                    Lore=new String[参数列表.length-2];
                    for(int i=0;i<参数列表.length-2;i++)
                        Lore[i]=参数列表[2+i];
                    玩家.setItemInHand(修改物品数据(玩家.getItemInHand(), 参数列表[1], Lore));
                }
                玩家.setItemInHand(修改物品数据(玩家.getItemInHand(), 参数列表[1]));
                break;
            case "setmap":
                ItemStack 物品=玩家.getItemInHand();
                if(物品.getType()==Material.MAP)
                    if(物品.hasItemMeta())
                        if(物品.getItemMeta().hasDisplayName())
                        {
                            ItemMeta 宝藏图属性=藏宝图物品.getItemMeta();
                            藏宝图名称=物品.getItemMeta().getDisplayName();
                            宝藏图属性.setDisplayName(藏宝图名称);
                            if(物品.getItemMeta().hasLore()){
                                宝藏图属性.setLore(物品.getItemMeta().getLore());
                            }
                            藏宝图物品.setItemMeta(宝藏图属性);
                            玩家.sendMessage("设置成功");
                            return true;
                        }
                玩家.sendMessage("必须手持自定义过的地图");
                break;
            case "save":
                玩家.sendMessage("保存完成");
                保存宝藏物品();
                break;
        }
        return true;
   }
}
