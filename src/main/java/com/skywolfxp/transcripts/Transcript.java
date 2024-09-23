package com.skywolfxp.transcripts;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import static com.skywolfxp.transcripts.Formatter.*;

/**
 * Created by SkyWolfXP
 * Project: jda-html-transcripts
 */
public class Transcript {

private static final List<String> VIDEO_FORMATS = List.of("mp4", "webm", "mkv", "avi", "mov", "flv", "wmv", "mpg", "mpeg");
private static final List<String> IMAGE_FORMATS = List.of("png", "jpg", "jpeg", "gif");
private static final List<String> AUDIO_FORMATS = List.of("mp3", "wav", "ogg", "flac");
private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MM/d/yyyy H:mm a (O)")
                                                                         .withZone(ZoneId.systemDefault());

public FileUpload createTranscript(@NotNull TextChannel textChannel) throws IOException {
  return FileUpload.fromData(generateFromMessages(textChannel.getIterableHistory().stream().toList()), "transcript.html");
}

public byte[] generateFromMessages(@NotNull List<Message> messages) throws IOException {
  if (messages.isEmpty()) {
    throw new IllegalArgumentException("No messages to generate a transcript from");
  }
  
  TextChannel channel = messages.iterator().next().getChannel().asTextChannel();
  Guild guild = channel.getGuild();
  Document doc = Jsoup.parse(getClass().getClassLoader().getResourceAsStream("transcript.html"), "UTF-8", "");
  doc.outputSettings().indentAmount(0).prettyPrint(true);
  
  if (guild.getIconUrl() != null) {
    doc.getElementsByClass("preamble__guild-icon").attr("src", guild.getIconUrl());
  }
  
  doc.getElementById("ticketTitle").text("#%s | %s".formatted(channel.getName(), guild.getName()));
  doc.getElementById("guildName").text(guild.getName());
  doc.getElementById("ticketName").text("#%s".formatted(channel.getName()));
  
  Element elChatLog = doc.getElementById("chatlog");
  
  for (Message msg : messages.stream().sorted(Comparator.comparing(ISnowflake::getTimeCreated)).toList()) {
    // Message Group
    Element elMessageGroup = doc.createElement("div").addClass("chatlog__message-group");
    
    // Referenced Message
    if (msg.getReferencedMessage() != null) {
      Element elReference = doc.createElement("div").addClass("chatlog__reference");
      Element elReferenceSymbol = doc.createElement("div").addClass("chatlog__reference-symbol");
      
      Message referencedMessage = msg.getReferencedMessage();
      User author = referencedMessage.getAuthor();
      
      guild.retrieveMemberById(author.getIdLong()).queue(
              (member) -> elReference.html("""
                                           <img class="chatlog__reference-avatar" src="%s" alt="Avatar" loading="lazy">
                                           <span class="chatlog__reference-name" title="%s" style="color: %s">%s</span>
                                           <div class="chatlog__reference-content">
                                           <span class="chatlog__reference-link" onclick="scrollToMessage(event, '%s')">
                                           <em style="font-style: italic;">
                                           %s
                                           </em>
                                           </span>
                                           </div>
                                           """.formatted(author.getAvatarUrl(), author.getName(),
                                                         member.getColor(), author.getName(),
                                                         referencedMessage.getId(),
                                                         referencedMessage.getContentDisplay().length() > 42
                                                                 ? referencedMessage.getContentDisplay()
                                                                                    .substring(0, 42) + "..."
                                                                 : " Click to see attachment %s".formatted(
                                                                         doc.createElement("img")
                                                                            .attr("src",
                                                                                  "https://svgshare.com/i/15DK.svg")))),
              (err) ->
              {
                throw new IllegalArgumentException("Can't Generate Transcript");
              });
      
      elMessageGroup.appendChild(elReferenceSymbol);
      elMessageGroup.appendChild(elReference);
    }
    
    // Messages
    Element elMessages = doc.createElement("div").addClass("chatlog__messages");
    
    // Timestamp
    Element elTimestamp = doc.createElement("span")
                             .addClass("chatlog__timestamp")
                             .text("%s".formatted(msg.getTimeCreated().format(TIME_FORMATTER)));
    
    Element elMessage = doc.createElement("div")
                           .addClass("chatlog__message")
                           .attr("data-message-id", msg.getId())
                           .attr("id", "message-%s".formatted(msg.getId()))
                           .attr("title", "Message sent: %s".formatted(msg.getTimeCreated().format(TIME_FORMATTER)));
    
    // System Message
    if (msg.getType().equals(MessageType.CHANNEL_PINNED_ADD)) {
      Element elSystemMessage = doc.createElement("div")
                                   .addClass("chatlog__system-message");
      
      Element elSystemMessagePinned = doc.createElement("div")
                                         .addClass("chatlog__system-message__pinned")
                                         .addClass("chatlog__reference-avatar");
      
      elSystemMessage.appendChild(elSystemMessagePinned);
      elMessageGroup.appendChild(elSystemMessage);
      
      Element elMessageContentDisplay = doc.createElement("div").addClass("chatlog__content");
      
      Element elMessageContentDisplayMarkdown = doc.createElement("div").addClass("markdown");
      
      Element elMessageContentDisplayMarkdownSpan = doc.createElement("span")
                                                       .addClass("preserve-whitespace")
                                                       .html("""
                                                             <b style="color:white;">%s</b> pinned a message to this channel. %s
                                                             """.formatted(msg.getAuthor().getName(), elTimestamp));
      
      elMessageContentDisplayMarkdown.appendChild(elMessageContentDisplayMarkdownSpan);
      elMessageContentDisplay.appendChild(elMessageContentDisplayMarkdown);
      elMessage.appendChild(elMessageContentDisplay);
    } else {
      User user = msg.getAuthor();
      
      Element elAuthorContainer = doc.createElement("div")
                                     .addClass("chatlog__author-avatar-container");
      
      Element elAuthorAvatar = doc.createElement("img")
                                  .addClass("chatlog__author-avatar")
                                  .attr("src", user.getEffectiveAvatarUrl())
                                  .attr("alt", "Avatar")
                                  .attr("loading", "lazy");
      
      Element elAuthorName = doc.createElement("span")
                                .addClass("chatlog__author-name")
                                .attr("title", user.getName())
                                .attr("data-user-id", user.getId())
                                .text(user.getName());
      
      elAuthorContainer.appendChild(elAuthorAvatar);
      elMessageGroup.appendChild(elAuthorContainer);
      elMessages.appendChild(elAuthorName);
      
      if (user.isBot()) {
        Element elBotTag = doc.createElement("span")
                              .addClass("chatlog__bot-tag")
                              .text("BOT");
        
        elMessages.appendChild(elBotTag);
      }
      
      elMessages.appendChild(elTimestamp);
    }
    
    // Message Content
    if (!msg.getContentDisplay().isEmpty()) {
      Element elMessageContentDisplay = doc.createElement("div").addClass("chatlog__content");
      
      Element elMessageContentDisplayMarkdown = doc.createElement("div").addClass("markdown");
      
      Element elMessageContentDisplayMarkdownSpan = doc.createElement("span")
                                                       .addClass("preserve-whitespace")
                                                       .html(format(msg.getContentDisplay()));
      
      elMessageContentDisplayMarkdown.appendChild(elMessageContentDisplayMarkdownSpan);
      elMessageContentDisplay.appendChild(elMessageContentDisplayMarkdown);
      elMessage.appendChild(elMessageContentDisplay);
    }
    
    // Attachments
    if (!msg.getAttachments().isEmpty()) {
      for (Message.Attachment attachment : msg.getAttachments()) {
        Element elAttachment = doc.createElement("div").addClass("chatlog__attachment");
        
        if (IMAGE_FORMATS.contains(attachment.getFileExtension())) {
          Element elAttachmentLink = doc.createElement("a");
          Element elAttachmentImage = doc.createElement("img")
                                         .addClass("chatlog__attachment-media")
                                         .attr("src", attachment.getUrl())
                                         .attr("alt", "Image attachment")
                                         .attr("loading", "lazy")
                                         .attr("title", "Image: %s%s".formatted(
                                                 attachment.getFileName(), formatBytes(attachment.getSize())));
          
          elAttachmentLink.appendChild(elAttachmentImage);
          elAttachment.appendChild(elAttachmentLink);
        } else if (VIDEO_FORMATS.contains(attachment.getFileExtension())) {
          Element elAttachmentVideo = doc.createElement("video")
                                         .addClass("chatlog__attachment-media")
                                         .attr("src", attachment.getUrl())
                                         .attr("alt", "Video attachment")
                                         .attr("controls", true)
                                         .attr("title", "Video: %s%s".formatted(
                                                 attachment.getFileName(), formatBytes(attachment.getSize())));
          
          elAttachment.appendChild(elAttachmentVideo);
        } else if (AUDIO_FORMATS.contains(attachment.getFileExtension())) {
          Element elAttachmentAudio = doc.createElement("audio")
                                         .addClass("chatlog__attachment-media")
                                         .attr("src", attachment.getUrl())
                                         .attr("alt", "Audio attachment")
                                         .attr("controls", true)
                                         .attr("title", "Audio: %s%s".formatted(
                                                 attachment.getFileName(), formatBytes(attachment.getSize())));
          
          elAttachment.appendChild(elAttachmentAudio);
        } else {
          Element elAttachmentGeneric = doc.createElement("div").addClass("chatlog__attachment-generic");
          
          Element elAttachmentGenericIcon = doc.createElement("svg").addClass("chatlog__attachment-generic-icon");
          Element elAttachmentGenericIconUse = doc.createElement("use").attr("xlink:href", "#icon-attachment");
          
          elAttachmentGeneric.appendChild(elAttachmentGenericIcon);
          elAttachmentGenericIcon.appendChild(elAttachmentGenericIconUse);
          
          Element elAttachmentGenericName = doc.createElement("div").addClass("chatlog__attachment-generic-name");
          Element elAttachmentGenericNameLink = doc.createElement("a")
                                                   .attr("href", attachment.getUrl())
                                                   .text(attachment.getFileName());
          
          elAttachmentGenericName.appendChild(elAttachmentGenericNameLink);
          elAttachmentGeneric.appendChild(elAttachmentGenericName);
          
          Element elAttachmentGenericSize = doc.createElement("div")
                                               .addClass("chatlog__attachment-generic-size")
                                               .text(formatBytes(attachment.getSize()));
          
          elAttachmentGeneric.appendChild(elAttachmentGenericSize);
          elAttachment.appendChild(elAttachmentGeneric);
        }
        
        elMessage.appendChild(elAttachment);
      }
    }
    
    elMessages.appendChild(elMessage);
    
    // Embeds
    if (!msg.getEmbeds().isEmpty()) {
      for (MessageEmbed embed : msg.getEmbeds()) {
        Element elEmbed = doc.createElement("div").addClass("chatlog__embed");
        
        // Embed Color
        Element elEmbedColor = doc.createElement("div").addClass("chatlog__embed-color-pill");
        
        if (embed.getColor() != null) {
          elEmbedColor.attr("style", "background-color: #%s".formatted(toHex(embed.getColor())));
        }
        
        elEmbed.appendChild(elEmbedColor);
        
        Element elEmbedContentContainer = doc.createElement("div").addClass("chatlog__embed-content-container");
        Element elEmbedContent = doc.createElement("div").addClass("chatlog__embed-content");
        Element elEmbedText = doc.createElement("div").addClass("chatlog__embed-text");
        
        // Embed Author
        if ((embed.getAuthor() != null) && (embed.getAuthor().getName() != null)) {
          Element elEmbedAuthor = doc.createElement("div").addClass("chatlog__embed-author");
          
          if (embed.getAuthor().getIconUrl() != null) {
            Element elEmbedAuthorIcon = doc.createElement("img")
                                           .addClass("chatlog__embed-author-icon")
                                           .attr("src", embed.getAuthor().getIconUrl())
                                           .attr("alt", "Author icon")
                                           .attr("loading", "lazy");
            
            elEmbedAuthor.appendChild(elEmbedAuthorIcon);
          }
          
          Element elEmbedAuthorName = doc.createElement("span").addClass("chatlog__embed-author-name");
          
          if (embed.getAuthor().getUrl() != null) {
            Element elEmbedAuthorNameLink = doc.createElement("a")
                                               .addClass("chatlog__embed-author-name-link")
                                               .attr("href", embed.getAuthor().getUrl())
                                               .text(embed.getAuthor().getName());
            
            elEmbedAuthorName.appendChild(elEmbedAuthorNameLink);
          } else {
            elEmbedAuthorName.text(embed.getAuthor().getName());
          }
          
          elEmbedAuthor.appendChild(elEmbedAuthorName);
          elEmbedText.appendChild(elEmbedAuthor);
        }
        
        // Embed Title
        if (embed.getTitle() != null) {
          Element elEmbedTitle = doc.createElement("div").addClass("chatlog__embed-title");
          
          if (embed.getUrl() != null) {
            Element elEmbedTitleLink = doc.createElement("a")
                                          .addClass("chatlog__embed-title-link")
                                          .attr("href", embed.getUrl());
            
            Element elEmbedTitleMarkdown = doc.createElement("div")
                                              .addClass("markdown")
                                              .html(format(embed.getTitle()));
            
            elEmbedTitleLink.appendChild(elEmbedTitleMarkdown);
            elEmbedTitle.appendChild(elEmbedTitleLink);
          } else {
            Element elEmbedTitleMarkdown = doc.createElement("div")
                                              .addClass("markdown")
                                              .html(format(embed.getTitle()));
            
            elEmbedTitle.appendChild(elEmbedTitleMarkdown);
          }
          
          elEmbedText.appendChild(elEmbedTitle);
        }
        
        // Embed Description
        if (embed.getDescription() != null) {
          Element elEmbedDescription = doc.createElement("div")
                                          .addClass("chatlog__embed-description");
          
          Element elEmbedDescriptionMarkdown = doc.createElement("div")
                                                  .addClass("markdown")
                                                  .html(format(embed.getDescription()));
          
          elEmbedDescription.appendChild(elEmbedDescriptionMarkdown);
          elEmbedText.appendChild(elEmbedDescription);
        }
        
        // Embed Field
        if (!embed.getFields().isEmpty()) {
          Element elEmbedFields = doc.createElement("div").addClass("chatlog__embed-fields");
          
          for (MessageEmbed.Field field : embed.getFields()) {
            Element elEmbedField = doc.createElement("div");
            elEmbedField.addClass(field.isInline() ? "chatlog__embed-field-inline" : "chatlog__embed-field");
            
            // Field Name
            if (field.getName() != null) {
              Element elEmbedFieldName = doc.createElement("div").addClass("chatlog__embed-field-name");
              
              Element elEmbedFieldNameMarkdown = doc.createElement("div")
                                                    .addClass("markdown")
                                                    .html(field.getName());
              
              elEmbedFieldName.appendChild(elEmbedFieldNameMarkdown);
              elEmbedField.appendChild(elEmbedFieldName);
            }
            
            // Field Value
            if (field.getValue() != null) {
              Element elEmbedFieldValue = doc.createElement("div").addClass("chatlog__embed-field-value");
              
              Element elEmbedFieldValueMarkdown = doc.createElement("div")
                                                     .addClass("markdown")
                                                     .html(format(field.getValue()));
              
              elEmbedFieldValue.appendChild(elEmbedFieldValueMarkdown);
              elEmbedField.appendChild(elEmbedFieldValue);
              
              elEmbedFields.appendChild(elEmbedField);
            }
          }
          
          elEmbedText.appendChild(elEmbedFields);
        }
        
        elEmbedContent.appendChild(elEmbedText);
        
        // Embed Thumbnail
        if ((embed.getThumbnail() != null) && (embed.getThumbnail().getUrl() != null)) {
          Element elEmbedThumbnail = doc.createElement("div").addClass("chatlog__embed-thumbnail-container");
          
          Element elEmbedThumbnailLink = doc.createElement("a")
                                            .addClass("chatlog__embed-thumbnail-link")
                                            .attr("href", embed.getThumbnail().getUrl());
          
          Element elEmbedThumbnailImage = doc.createElement("img")
                                             .addClass("chatlog__embed-thumbnail")
                                             .attr("src", embed.getThumbnail().getUrl())
                                             .attr("alt", "Thumbnail")
                                             .attr("loading", "lazy");
          
          elEmbedThumbnailLink.appendChild(elEmbedThumbnailImage);
          elEmbedThumbnail.appendChild(elEmbedThumbnailLink);
          
          elEmbedContent.appendChild(elEmbedThumbnail);
        }
        
        elEmbedContentContainer.appendChild(elEmbedContent);
        
        // Embed Image
        if ((embed.getImage() != null) && (embed.getImage().getUrl() != null)) {
          Element elEmbedImage = doc.createElement("div").addClass("chatlog__embed-image-container");
          
          Element elEmbedImageLink = doc.createElement("a")
                                        .addClass("chatlog__embed-image-link")
                                        .attr("href", embed.getImage().getUrl());
          
          Element elEmbedImageImage = doc.createElement("img")
                                         .addClass("chatlog__embed-image")
                                         .attr("src", embed.getImage().getUrl())
                                         .attr("alt", "Image")
                                         .attr("loading", "lazy");
          
          elEmbedImageLink.appendChild(elEmbedImageImage);
          elEmbedImage.appendChild(elEmbedImageLink);
          
          elEmbedContentContainer.appendChild(elEmbedImage);
        }
        
        // Embed Footer
        if (embed.getFooter() != null) {
          Element elEmbedFooter = doc.createElement("div").addClass("chatlog__embed-footer");
          
          if (embed.getFooter().getIconUrl() != null) {
            Element elEmbedFooterIcon = doc.createElement("img")
                                           .addClass("chatlog__embed-footer-icon")
                                           .attr("src", embed.getFooter().getIconUrl())
                                           .attr("alt", "Footer icon")
                                           .attr("loading", "lazy");
            
            elEmbedFooter.appendChild(elEmbedFooterIcon);
          }
          
          if (embed.getFooter().getText() != null) {
            Element elEmbedFooterText = doc.createElement("span")
                                           .addClass("chatlog__embed-footer-text")
                                           .text(embed.getTimestamp() != null
                                                         ? "%s • %s".formatted(
                                                   embed.getFooter().getText(),
                                                   embed.getTimestamp().format(TIME_FORMATTER))
                                                         : embed.getFooter().getText());
            
            elEmbedFooter.appendChild(elEmbedFooterText);
          }
          
          elEmbedContentContainer.appendChild(elEmbedFooter);
        }
        
        elEmbed.appendChild(elEmbedContentContainer);
        elMessages.appendChild(elEmbed);
      }
    }
    
    Element elActionRow = doc.createElement("div").addClass("chatlog__action-row");
    
    // Buttons
    if (!msg.getButtons().isEmpty()) {
      for (Button button : msg.getButtons()) {
        Element elButton = doc.createElement("div").addClass("chatlog__action-row__button");
        
        switch (button.getStyle()) {
          case PRIMARY -> elButton.addClass("chatlog__action-row__button--primary");
          case SECONDARY -> elButton.addClass("chatlog__action-row__button--secondary");
          case SUCCESS -> elButton.addClass("chatlog__action-row__button--success");
          case DANGER -> elButton.addClass("chatlog__action-row__button--danger");
          case LINK -> elButton.addClass("chatlog__action-row__button--link");
        }
        
        Element elButtonLabel = doc.createElement("span")
                                   .text(button.getLabel());
        
        Element elButtonEmoji = doc.createElement("span")
                                   .addClass("chatlog__action-row__button__emoji")
                                   .text(button.getEmoji() == null ? "" : button.getEmoji().getName());
        
        elButton.appendChild(elButtonEmoji)
                .appendChild(elButtonLabel);
        elActionRow.appendChild(elButton);
        elMessages.appendChild(elActionRow);
      }
    }
    
    elMessageGroup.appendChild(elMessages);
    elChatLog.appendChild(elMessageGroup);
  }
  
  return doc.outerHtml().getBytes();
}
}