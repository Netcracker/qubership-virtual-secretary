package com.netcracker.qubership.vsec.deepseek;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter @Setter
public class DeepSeekChatResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("object")
    private String object;

    @JsonProperty("created")
    private long created;

    @JsonProperty("model")
    private String model;

    @JsonProperty("choices")
    private List<Choice> choices;

    @JsonProperty("usage")
    private Usage usage;

    @JsonProperty("system_fingerprint")
    private String systemFingerprint;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter @Setter
    public static class Choice {

        @JsonProperty("index")
        private int index;

        @JsonProperty("message")
        private Message message;

        @JsonProperty("logprobs")
        private Object logprobs; // Can be null or a complex object

        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter @Setter
    public static class Message {

        @JsonProperty("role")
        private String role;

        @JsonProperty("content")
        private String content;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter @Setter
    public static class Usage {

        @JsonProperty("prompt_tokens")
        private int promptTokens;

        @JsonProperty("completion_tokens")
        private int completionTokens;

        @JsonProperty("total_tokens")
        private int totalTokens;

        @JsonProperty("prompt_tokens_details")
        private PromptTokensDetails promptTokensDetails;

        @JsonProperty("prompt_cache_hit_tokens")
        private int promptCacheHitTokens;

        @JsonProperty("prompt_cache_miss_tokens")
        private int promptCacheMissTokens;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter @Setter
    public static class PromptTokensDetails {

        @JsonProperty("cached_tokens")
        private int cachedTokens;
    }
}