package de.videoscreenpro;

import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public final class VideoScreenPro extends JavaPlugin implements Listener, CommandExecutor {
  private final Map<String, Screen> screens = new HashMap<>();
  private final Map<UUID, String> selected = new HashMap<>();
  private NamespacedKey remoteKey;
  private final ExecutorService workers = Executors.newCachedThreadPool();
  private record Screen(String name, Location origin, BlockFace facing, int width, int height, List<UUID> frames, List<MapView> maps) {}
  private final Map<String, Playback> playing = new ConcurrentHashMap<>();
  private static final class Playback { volatile boolean active=true; Process process; BukkitTask display; }

  @Override public void onEnable() {
    saveDefaultConfig(); remoteKey=new NamespacedKey(this,"remote");
    Objects.requireNonNull(getCommand("screen")).setExecutor(this);
    getServer().getPluginManager().registerEvents(this,this);
    new File(getDataFolder(),"videos").mkdirs(); loadScreens();
  }
  @Override public void onDisable() { for(String name: new ArrayList<>(playing.keySet())) stop(name); saveScreens(); workers.shutdownNow(); }
  private ItemStack remote() {
    ItemStack item=new ItemStack(Material.CARROT_ON_A_STICK); ItemMeta meta=item.getItemMeta();
    meta.setDisplayName("§b§lVideoScreen §fFernbedienung");
    meta.setItemModel(new NamespacedKey(this,"remote"));
    meta.getPersistentDataContainer().set(remoteKey,PersistentDataType.BYTE,(byte)1);
    item.setItemMeta(meta); return item;
  }
  private boolean isRemote(ItemStack item) { return item!=null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(remoteKey,PersistentDataType.BYTE); }
  private void say(CommandSender s,String msg){s.sendMessage("§b[VideoScreen] §f"+msg);}
  @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
    if(!(sender instanceof Player p)){say(sender,"Nur im Spiel verwendbar.");return true;}
    if(!p.hasPermission("videoscreen.admin")){say(p,"Keine Berechtigung.");return true;}
    if(args.length==0 || args[0].equalsIgnoreCase("help")) {say(p,"/screen remote | create <name> <breite> <höhe> | select <name> | play <datei/URL> | live <URL> | stop | resize <breite> <höhe> | remove <name> | list");return true;}
    try {
      switch(args[0].toLowerCase(Locale.ROOT)) {
        case "remote" -> {p.getInventory().addItem(remote());say(p,"Fernbedienung erhalten. Rechtsklick öffnet das Menü.");}
        case "list" -> say(p,"Bildschirme: "+String.join(", ",screens.keySet()));
        case "select" -> {Screen sc=requireScreen(args[1]);selected.put(p.getUniqueId(),sc.name());say(p,"Ausgewählt: "+sc.name());}
        case "create" -> {String name=args[1].toLowerCase(Locale.ROOT);int w=Integer.parseInt(args[2]),h=Integer.parseInt(args[3]);validateSize(w,h);if(screens.containsKey(name))throw new IllegalArgumentException("Name bereits vergeben");BlockFace face=p.getFacing().getOppositeFace();Location loc=p.getLocation().getBlock().getLocation().add(0,1,0);Screen sc=spawn(name,loc,face,w,h);screens.put(name,sc);selected.put(p.getUniqueId(),name);saveScreens();say(p,"Bildschirm erstellt: "+name+" ("+w+"x"+h+")");}
        case "play","live" -> {Screen sc=current(p);String source=String.join(" ",Arrays.copyOfRange(args,1,args.length));play(sc,source);say(p,"Quelle wird geladen. Fehlermeldungen stehen im Serverlog.");}
        case "stop" -> {Screen sc=current(p);stop(sc.name());say(p,"Gestoppt.");}
        case "resize" -> {Screen sc=current(p);int w=Integer.parseInt(args[1]),h=Integer.parseInt(args[2]);validateSize(w,h);stop(sc.name());removeFrames(sc);Screen resized=spawn(sc.name(),sc.origin(),sc.facing(),w,h);screens.put(sc.name(),resized);saveScreens();say(p,"Neue Größe: "+w+"x"+h);}
        case "remove" -> {Screen sc=requireScreen(args[1]);stop(sc.name());removeFrames(sc);screens.remove(sc.name());saveScreens();say(p,"Entfernt.");}
        default -> say(p,"Unbekannter Befehl. /screen help");
      }
    } catch(Exception e){say(p,"Fehler: "+e.getMessage());}
    return true;
  }
  private Screen requireScreen(String name){Screen s=screens.get(name.toLowerCase(Locale.ROOT));if(s==null)throw new IllegalArgumentException("Bildschirm nicht gefunden");return s;}
  private Screen current(Player p){String n=selected.get(p.getUniqueId());if(n==null)throw new IllegalArgumentException("Zuerst /screen select <name>");return requireScreen(n);}
  private void validateSize(int w,int h){if(w<1||h<1||w>getConfig().getInt("max-width",8)||h>getConfig().getInt("max-height",5))throw new IllegalArgumentException("Ungültige Größe");}
  private Screen spawn(String name,Location origin,BlockFace face,int w,int h){
    if(face!=BlockFace.NORTH&&face!=BlockFace.SOUTH&&face!=BlockFace.EAST&&face!=BlockFace.WEST)throw new IllegalArgumentException("Nur horizontale Ausrichtung");
    List<UUID> ids=new ArrayList<>();List<MapView> maps=new ArrayList<>();
    for(int y=0;y<h;y++)for(int x=0;x<w;x++){
      int dx=(face==BlockFace.NORTH?x:face==BlockFace.SOUTH?-x:0);
      int dz=(face==BlockFace.EAST?x:face==BlockFace.WEST?-x:0);
      Location at=origin.clone().add(dx,h-1-y,dz);
      MapView mv=Bukkit.createMap(Objects.requireNonNull(at.getWorld()));mv.getRenderers().forEach(mv::removeRenderer);
      mv.addRenderer(new PixelRenderer());maps.add(mv);
      ItemFrame frame=at.getWorld().spawn(at,ItemFrame.class,f->{f.setFacingDirection(face,true);f.setFixed(true);f.setInvulnerable(true);f.setVisible(false);});
      ItemStack map=new ItemStack(Material.FILLED_MAP);MapMeta meta=(MapMeta)map.getItemMeta();meta.setMapView(mv);map.setItemMeta(meta);frame.setItem(map,false);ids.add(frame.getUniqueId());
    }
    return new Screen(name,origin,face,w,h,ids,maps);
  }
  private void removeFrames(Screen s){for(UUID id:s.frames()){Entity e=Bukkit.getEntity(id);if(e!=null)e.remove();}}
  private static final class PixelRenderer extends MapRenderer {
    volatile BufferedImage image;
    PixelRenderer(){super(false);}
    @Override public void render(MapView map,MapCanvas canvas,Player player){BufferedImage i=image;if(i!=null)canvas.drawImage(0,0,i);}
  }
  private void play(Screen sc,String source) throws IOException {
    if(source.isBlank())throw new IllegalArgumentException("Quelle fehlt");stop(sc.name());
    boolean url=source.startsWith("https://")||source.startsWith("http://");
    if(!url){Path root=new File(getDataFolder(),"videos").toPath().toRealPath();Path path=root.resolve(source).normalize();if(!path.startsWith(root)||!Files.isRegularFile(path))throw new IllegalArgumentException("Datei fehlt in videos/");source=path.toString();}
    final String input=source;Playback pb=new Playback();playing.put(sc.name(),pb);
    workers.submit(()->{
      try{
        String resolved=input;
        if(input.startsWith("https://www.youtube.com/")||input.startsWith("https://youtu.be/")||input.startsWith("https://www.twitch.tv/")){
          Process resolver=new ProcessBuilder(getConfig().getString("ytdlp","yt-dlp"),"--no-playlist","-g",input).redirectErrorStream(false).start();
          boolean ended=resolver.waitFor(25,TimeUnit.SECONDS);if(!ended){resolver.destroyForcibly();throw new IOException("yt-dlp timeout");}
          if(resolver.exitValue()!=0)throw new IOException("yt-dlp failed");
          resolved=new String(resolver.getInputStream().readAllBytes()).lines().findFirst().orElseThrow();
        }
        int width=sc.width()*128,height=sc.height()*128,fps=Math.max(1,Math.min(10,getConfig().getInt("fps",4)));
        List<String> cmd=List.of(getConfig().getString("ffmpeg","ffmpeg"),"-nostdin","-loglevel","error","-i",resolved,"-vf","scale="+width+":"+height+",fps="+fps,"-an","-pix_fmt","rgb24","-f","rawvideo","pipe:1");
        pb.process=new ProcessBuilder(cmd).start();
        Process process=pb.process;workers.submit(()->{try(InputStream err=process.getErrorStream()){byte[] data=err.readNBytes(4096);if(data.length>0)getLogger().warning("FFmpeg: "+new String(data));}catch(IOException ignored){}});
        byte[] bytes=new byte[width*height*3];long interval=1000L/fps;
        try(InputStream stream=process.getInputStream()){
          while(pb.active){long start=System.currentTimeMillis();int n=0;while(n<bytes.length){int r=stream.read(bytes,n,bytes.length-n);if(r<0)break;n+=r;}if(n<bytes.length)break;
            BufferedImage frame=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
            int[] rgb=new int[width*height];for(int i=0,j=0;i<rgb.length;i++){rgb[i]=((bytes[j++]&255)<<16)|((bytes[j++]&255)<<8)|(bytes[j++]&255);}frame.setRGB(0,0,width,height,rgb,0,width);
            Bukkit.getScheduler().runTask(this,()->{if(!pb.active)return;for(int y=0;y<sc.height();y++)for(int x=0;x<sc.width();x++){
              MapRenderer r=sc.maps().get(y*sc.width()+x).getRenderers().getFirst();if(r instanceof PixelRenderer pr)pr.image=frame.getSubimage(x*128,y*128,128,128);
            }});
            long wait=interval-(System.currentTimeMillis()-start);if(wait>0)Thread.sleep(wait);
          }
        }
      }catch(Exception e){getLogger().warning("Playback "+sc.name()+": "+e.getMessage());}finally{stop(sc.name());}
    });
  }
  private void stop(String name){Playback p=playing.remove(name);if(p!=null){p.active=false;if(p.process!=null)p.process.destroyForcibly();}}
  @EventHandler public void onUse(PlayerInteractEvent e){if(!e.getAction().isRightClick())return;if(!isRemote(e.getItem()))return;e.setCancelled(true);Player p=e.getPlayer();if(!p.hasPermission("videoscreen.admin"))return;
    Inventory inv=Bukkit.createInventory(null,27,"VideoScreen Fernbedienung");
    menuItem(inv,10,Material.LIME_DYE,"§aVideo starten: /screen play <Datei/URL>");menuItem(inv,12,Material.RED_DYE,"§cStopp");menuItem(inv,14,Material.MAP,"§eBildschirm auswählen: /screen select");menuItem(inv,16,Material.PAPER,"§bGröße: /screen resize <B> <H>");p.openInventory(inv);
  }
  private void menuItem(Inventory inv,int slot,Material material,String title){ItemStack item=new ItemStack(material);ItemMeta m=item.getItemMeta();m.setDisplayName(title);item.setItemMeta(m);inv.setItem(slot,item);}
  @EventHandler public void onClick(InventoryClickEvent e){if(!e.getView().getTitle().equals("VideoScreen Fernbedienung"))return;e.setCancelled(true);if(!(e.getWhoClicked() instanceof Player p)||!p.hasPermission("videoscreen.admin"))return;
    if(e.getRawSlot()==12){try{stop(current(p).name());say(p,"Gestoppt.");}catch(Exception ex){say(p,ex.getMessage());}p.closeInventory();}
    else if(e.getRawSlot()==10||e.getRawSlot()==14||e.getRawSlot()==16){p.closeInventory();say(p,"Verwende den angezeigten /screen-Befehl im Chat.");}
  }
  private void saveScreens(){getConfig().set("screens",null);for(Screen s:screens.values()){String base="screens."+s.name()+".";getConfig().set(base+"location",s.origin());getConfig().set(base+"facing",s.facing().name());getConfig().set(base+"width",s.width());getConfig().set(base+"height",s.height());getConfig().set(base+"frames",s.frames().stream().map(UUID::toString).toList());getConfig().set(base+"maps",s.maps().stream().map(MapView::getId).toList());}saveConfig();}
  private void loadScreens(){ConfigurationSection section=getConfig().getConfigurationSection("screens");if(section==null)return;for(String name:section.getKeys(false))try{String base="screens."+name+".";Location loc=getConfig().getLocation(base+"location");if(loc==null||loc.getWorld()==null)continue;BlockFace face=BlockFace.valueOf(getConfig().getString(base+"facing","NORTH"));int w=getConfig().getInt(base+"width"),h=getConfig().getInt(base+"height");List<UUID> ids=new ArrayList<>();for(String id:getConfig().getStringList(base+"frames"))ids.add(UUID.fromString(id));List<MapView> maps=new ArrayList<>();for(int id:getConfig().getIntegerList(base+"maps")){MapView mv=Bukkit.getMap(id);if(mv==null)throw new IllegalStateException("Missing map "+id);mv.getRenderers().forEach(mv::removeRenderer);mv.addRenderer(new PixelRenderer());maps.add(mv);}if(maps.size()!=w*h)throw new IllegalStateException("Map count mismatch");screens.put(name,new Screen(name,loc,face,w,h,ids,maps));}catch(Exception ex){getLogger().warning("Could not restore screen "+name+": "+ex.getMessage());}}
}
