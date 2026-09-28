package com.wavestudio.rtp.config;

import com.wavestudio.rtp.model.Dimension;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.util.Map;

public class MessageProvider {
    private final MiniMessage miniMessage;
    private final RtpConfig config;

    public MessageProvider(RtpConfig config) {
        this.config = config;
        this.miniMessage = MiniMessage.miniMessage();
    }

    public Component parse(String path) {
        return parse(path, Map.of());
    }

    public Component parse(String path, Map<String, String> placeholders) {
        String raw = config.getMessage(path, placeholders);
        return miniMessage.deserialize(raw, createResolvers(placeholders));
    }

    public Component parseRaw(String raw) {
        return miniMessage.deserialize(raw);
    }

    public Component parseRaw(String raw, Map<String, String> placeholders) {
        return miniMessage.deserialize(raw, createResolvers(placeholders));
    }

    public Component prefix() {
        return parse("prefix");
    }

    public Component prefixed(String path, Map<String, String> placeholders) {
        return prefix().append(Component.space()).append(parse(path, placeholders));
    }

    public Component prefixedRaw(String raw, Map<String, String> placeholders) {
        return prefix().append(Component.space()).append(parseRaw(raw, placeholders));
    }

    private TagResolver[] createResolvers(Map<String, String> placeholders) {
        TagResolver[] resolvers = new TagResolver[placeholders.size()];
        int i = 0;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            resolvers[i++] = Placeholder.unparsed(entry.getKey(), entry.getValue());
        }
        return resolvers;
    }

    public Component dimensionName(Dimension dimension) {
        String path = "dialog.dimensions." + dimension.getId();
        return parse(path);
    }

    public Component dimensionItemDescription(Dimension dimension) {
        String path = "dialog.item-descriptions." + dimension.getId();
        return parse(path);
    }
}