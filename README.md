# Discord JDA HTML Transcripts

[![Discord](https://img.shields.io/discord/1055244032105787472?style=for-the-badge&logo=discord&logoColor=%23ffffff&label=Discord&labelColor=%235865f2)](https://discord.gg/QmYE4Gngxz)
![JitPack](https://img.shields.io/jitpack/version/com.github.skywolfxp/jda-html-transcripts?style=for-the-badge&logo=jitpack&label=JitPack&labelColor=%2334495e)

Discord HTML Transcripts is a node.js module (recode on JDA) to generate nice looking HTML transcripts. Processes
discord markdown like **bold**, *italics*, ~~strikethroughs~~, and more. Nicely formats attachments and embeds. Built in
XSS protection, preventing users from inserting html tags.

**This module is designed to work with [JDA](https://github.com/DV8FromTheWorld/JDA).**

HTML Template stolen from [DiscordChatExporter](https://github.com/Tyrrrz/DiscordChatExporter).

## Installation

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

```xml
<dependency>
    <groupId>com.github.skywolfxp</groupId>
    <artifactId>jda-html-transcripts</artifactId>
    <version>VERSION</version>
</dependency>
```

## Usage

### Using JDA's built in message fetcher

```java
FileUpload transcript = new Transcript().createTranscript(textChannel);
```

## Output

![output](https://img.derock.dev/5f5q0a.png)

