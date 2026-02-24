package com.example.agentdroid.model

data class AiModel(
    val id: String,
    val displayName: String
)

enum class AiProvider(
    val displayName: String,
    val chatEndpointUrl: String,
    val models: List<AiModel>
) {
    OPENAI(
        displayName = "OpenAI",
        chatEndpointUrl = "https://api.openai.com/v1/chat/completions",
        models = listOf(
            AiModel("gpt-4o-mini", "GPT-4o Mini"),
            AiModel("gpt-4o", "GPT-4o"),
            AiModel("o3-mini", "o3-mini"),
        )
    ),
    GEMINI(
        displayName = "Google Gemini",
        chatEndpointUrl = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
        models = listOf(
            AiModel("gemini-2.0-flash", "Gemini 2.0 Flash"),
            AiModel("gemini-2.0-flash-lite", "Gemini 2.0 Flash Lite"),
            AiModel("gemini-1.5-flash", "Gemini 1.5 Flash"),
            AiModel("gemini-1.5-pro", "Gemini 1.5 Pro"),
        )
    ),
    CLAUDE(
        displayName = "Anthropic Claude",
        chatEndpointUrl = "https://api.anthropic.com/v1/messages",
        models = listOf(
            AiModel("claude-sonnet-4-20250514", "Claude Sonnet 4"),
            AiModel("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet"),
            AiModel("claude-3-5-haiku-20241022", "Claude 3.5 Haiku"),
        )
    ),
    DEEPSEEK(
        displayName = "DeepSeek",
        chatEndpointUrl = "https://api.deepseek.com/v1/chat/completions",
        models = listOf(
            AiModel("deepseek-chat", "DeepSeek V3"),
            AiModel("deepseek-reasoner", "DeepSeek R1"),
        )
    );

    val isOpenAiCompatible: Boolean
        get() = this != CLAUDE
}
