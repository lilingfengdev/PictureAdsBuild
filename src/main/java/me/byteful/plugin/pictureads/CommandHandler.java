package me.byteful.plugin.pictureads;

import org.apache.commons.validator.routines.UrlValidator;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import redempt.redlib.commandmanager.CommandHook;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public final class CommandHandler {
  private final PictureAdsPlugin plugin;

  public CommandHandler(PictureAdsPlugin plugin) {
    this.plugin = plugin;
  }

  @CommandHook("reload")
  public void onReload(CommandSender sender) {
    plugin.reloadConfig();
    plugin.loadMessages();
    plugin.scheduledAds.close();
    plugin.scheduledAds = new ScheduledAds(plugin);
    sender.sendMessage(plugin.getMessages().get("reload"));
  }

  @CommandHook("broadcast")
  public void onBroadcast(CommandSender sender, String image) {
    CompletableFuture.runAsync(() -> {
      BufferedImage bufferedImage;
      if (UrlValidator.getInstance().isValid(image)) {
        // is URL
        if (!plugin.getConfig().getBoolean("urls")) {
          sender.sendMessage(plugin.getMessages().get("urls_disabled"));

          return;
        }

        try {
          sender.sendMessage(plugin.getMessages().get("loading_url"));
          bufferedImage = ImageIO.read(new URL(image));
        } catch (IOException ignored) {
          sender.sendMessage(plugin.getMessages().get("failed_to_load_url"));
          return;
        }

        if (bufferedImage == null) {
          sender.sendMessage(plugin.getMessages().get("failed_to_load_url"));
          return;
        }
      } else {
        // maybe file
        try {
          sender.sendMessage(plugin.getMessages().get("loading_file"));
          bufferedImage = ImageIO.read(new File(plugin.getDataFolder(), image));
        } catch (IOException ignored) {
          sender.sendMessage(plugin.getMessages().get("failed_to_find_file"));
          return;
        }

        if (bufferedImage == null) {
          sender.sendMessage(plugin.getMessages().get("failed_to_find_file"));
          return;
        }
      }

      // loaded image, now need to set up renderer to display for all players
      Bukkit.getScheduler().runTask(plugin, () -> plugin.broadcast(bufferedImage));
    });
  }
}