package com.skywolfxp.transcripts;

import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by SkyWolfXP
 * Project: jda-html-transcripts
 */
public class Formatter
{
  private final static Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");
  private final static Pattern ITALIC = Pattern.compile("\\*(.+?)\\*");
  private final static Pattern STRIKE_THROUGH = Pattern.compile("~~(.+?)~~");
  private final static Pattern UNDERLINE = Pattern.compile("__(.+?)__");
  private final static Pattern CODE_BLOCK = Pattern.compile("```(.+?)```");
  private final static Pattern CODE_LINE = Pattern.compile("`(.+?)`");
  private final static Pattern LINE_BREAK = Pattern.compile("\\n");
  
  public static String formatBytes(long bytes)
  {
    int unit = 1024;
    
    if (bytes < unit) {return bytes + " B";}
    
    int exp = (int) (Math.log(bytes) / Math.log(unit));
    String pre = "KMGTPE".charAt(exp - 1) + "";
    
    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
  }
  
  public static String format(String text)
  {
    boolean foundCodeBlock = false;
    
    Matcher matcher = BOLD.matcher(text);
    while (matcher.find())
    {
      String group = matcher.group();
      text = text.replace(group, "<strong>%s</strong>".formatted(group.replace("**", "")));
    }
    
    matcher = ITALIC.matcher(text);
    while (matcher.find())
    {
      String group = matcher.group();
      text = text.replace(group, "<em>%s</em>".formatted(group.replace("*", "")));
    }
    
    matcher = STRIKE_THROUGH.matcher(text);
    while (matcher.find())
    {
      String group = matcher.group();
      text = text.replace(group, "<s>%s</s>".formatted(group.replace("~~", "")));
    }
    
    matcher = UNDERLINE.matcher(text);
    while (matcher.find())
    {
      String group = matcher.group();
      text = text.replace(group, "<u>%s</u>".formatted(group.replace("__", "")));
    }
    
    matcher = CODE_BLOCK.matcher(text);
    while (matcher.find())
    {
      String group = matcher.group();
      text = text.replace(group, "<div class=\"pre pre--multiline nohighlight\">%s</div>".formatted(group.replace("```", "")));
      foundCodeBlock = true;
    }
    
    if (!foundCodeBlock)
    {
      matcher = CODE_LINE.matcher(text);
      while (matcher.find())
      {
        String group = matcher.group();
        text = text.replace(group, "<span class=\"pre pre--inline\">%s</span>".formatted(group.replace("`", "")));
      }
    }
    
    matcher = LINE_BREAK.matcher(text);
    while (matcher.find())
    {
      text = text.replace(matcher.group(), "<br>");
    }
    
    return text;
  }
  
  public static String toHex(Color color)
  {
    String hex = Integer.toHexString(color.getRGB() & 0xffffff);
    
    while (hex.length() < 6) {hex = "0%s".formatted(hex);}
    
    return hex;
  }
}