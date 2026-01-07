package com.imu.verba.tts

/**
 * Document context types for context-aware pronunciation.
 * The preprocessor uses this to determine how to expand ambiguous abbreviations.
 */
enum class DocumentContext {
    /** Technical/programming documentation (ML = Machine Learning, API = A-P-I) */
    TECHNICAL,
    /** Medical/scientific documents (ML = milliliter, mg = milligram) */
    MEDICAL,
    /** General text - uses frequency-based defaults */
    GENERAL
}

/**
 * Context-aware text preprocessor for TTS.
 * 
 * Handles:
 * - Context-sensitive abbreviation expansion (ML → "machine learning" in tech, "milliliter" in medical)
 * - Technical term pronunciation (API spoken as "A-P-I", SQL as "sequel")
 * - Unit expansion (5GB → "5 gigabytes")
 * - Number handling (1.5 → "one point five")
 * - Code/path pronunciation improvements
 * 
 * Context detection is automatic based on document content analysis.
 */
class TtsSpeechPreprocessor {
    
    private var currentContext: DocumentContext = DocumentContext.GENERAL
    
    // Keywords that indicate technical context
    private val techIndicators = setOf(
        "function", "class", "method", "variable", "code", "programming",
        "algorithm", "database", "framework", "library", "repository",
        "neural network", "deep learning", "training", "model", "dataset",
        "tensorflow", "pytorch", "kubernetes", "docker", "android", "kotlin",
        "python", "javascript", "typescript", "rust", "java", "github"
    )
    
    // Keywords that indicate medical context
    private val medicalIndicators = setOf(
        "patient", "diagnosis", "treatment", "medication", "dosage",
        "prescription", "symptom", "clinical", "hospital", "doctor",
        "surgery", "blood", "plasma", "injection", "therapy", "dose"
    )
    
    // Context-sensitive abbreviations (tech meaning vs medical meaning)
    private val contextAbbreviations = mapOf(
        "ml" to mapOf(
            DocumentContext.TECHNICAL to "machine learning",
            DocumentContext.MEDICAL to "milliliter",
            DocumentContext.GENERAL to "machine learning"
        ),
        "ai" to mapOf(
            DocumentContext.TECHNICAL to "artificial intelligence",
            DocumentContext.MEDICAL to "artificial intelligence",
            DocumentContext.GENERAL to "A I"
        ),
        "dl" to mapOf(
            DocumentContext.TECHNICAL to "deep learning",
            DocumentContext.MEDICAL to "deciliter",
            DocumentContext.GENERAL to "deep learning"
        ),
        "nn" to mapOf(
            DocumentContext.TECHNICAL to "neural network",
            DocumentContext.MEDICAL to "neural network",
            DocumentContext.GENERAL to "neural network"
        ),
        "cv" to mapOf(
            DocumentContext.TECHNICAL to "computer vision",
            DocumentContext.MEDICAL to "cardiovascular",
            DocumentContext.GENERAL to "C V"
        ),
        "nlp" to mapOf(
            DocumentContext.TECHNICAL to "natural language processing",
            DocumentContext.MEDICAL to "neuro linguistic programming",
            DocumentContext.GENERAL to "N L P"
        )
    )
    
    // Always expand these the same way regardless of context
    private val universalAbbreviations = mapOf(
        // Tech acronyms - spell out
        "api" to "A P I",
        "cpu" to "C P U",
        "gpu" to "G P U",
        "tpu" to "T P U",
        "ram" to "RAM",
        "rom" to "ROM",
        "url" to "U R L",
        "uri" to "U R I",
        "html" to "H T M L",
        "css" to "C S S",
        "json" to "jason",
        "xml" to "X M L",
        "yaml" to "yammel",
        "sql" to "sequel",
        "nosql" to "no sequel",
        "gui" to "gooey",
        "cli" to "C L I",
        "ide" to "I D E",
        "sdk" to "S D K",
        "jdk" to "J D K",
        "jvm" to "J V M",
        "llm" to "L L M",
        "gpt" to "G P T",
        "rag" to "R A G",
        "pdf" to "P D F",
        "md" to "markdown",
        "tts" to "text to speech",
        "stt" to "speech to text",
        "ocr" to "O C R",
        "iot" to "I O T",
        "aws" to "A W S",
        "gcp" to "G C P",
        "sso" to "S S O",
        "oauth" to "O auth",
        "jwt" to "J W T",
        "http" to "H T T P",
        "https" to "H T T P S",
        "ftp" to "F T P",
        "ssh" to "S S H",
        "tcp" to "T C P",
        "udp" to "U D P",
        "ip" to "I P",
        "dns" to "D N S",
        "vpn" to "V P N",
        "ui" to "U I",
        "ux" to "U X",
        "cicd" to "C I C D",
        "ci/cd" to "C I C D",
        "devops" to "dev ops",
        "saas" to "sass",
        "paas" to "pass",
        "iaas" to "I ass",
        "mvvm" to "M V V M",
        "mvc" to "M V C",
        "crud" to "crud",
        "rest" to "rest",
        "grpc" to "G R P C",
        "graphql" to "graph Q L",
        "regex" to "reg ex",
        "npm" to "N P M",
        "nvm" to "N V M",
        
        // Common shortcuts
        "e.g." to "for example",
        "i.e." to "that is",
        "etc." to "et cetera",
        "vs." to "versus",
        "vs" to "versus",
        "w/" to "with",
        "w/o" to "without",
        "btw" to "by the way",
        "fyi" to "for your information",
        "asap" to "as soon as possible",
        "eta" to "E T A",
        "faq" to "F A Q",
        "diy" to "D I Y",
        "tldr" to "T L D R",
        "tl;dr" to "too long didn't read",
        
        // Units
        "kb" to "kilobytes",
        "mb" to "megabytes",
        "gb" to "gigabytes",
        "tb" to "terabytes",
        "pb" to "petabytes",
        "kbps" to "kilobits per second",
        "mbps" to "megabits per second",
        "gbps" to "gigabits per second",
        "ghz" to "gigahertz",
        "mhz" to "megahertz",
        "khz" to "kilohertz",
        "hz" to "hertz",
        "ms" to "milliseconds",
        "ns" to "nanoseconds",
        "px" to "pixels",
        "dpi" to "D P I",
        "fps" to "frames per second",
        
        // Medical units (when context is medical)
        "mg" to "milligram",
        "mcg" to "microgram",
        "kg" to "kilogram",
        "cm" to "centimeter",
        "mm" to "millimeter"
    )
    
    // Pattern replacements using regex
    private val patternReplacements = listOf(
        // File paths - add pauses
        Regex("""([/\\])""") to ", ",
        // Version numbers
        Regex("""v(\d+)\.(\d+)\.(\d+)""") to "version $1 point $2 point $3",
        Regex("""v(\d+)\.(\d+)""") to "version $1 point $2",
        // Code references like `function()` - remove backticks
        Regex("""`([^`]+)`""") to "$1",
        // Camel case splitting (getUserName → get User Name)
        Regex("""([a-z])([A-Z])""") to "$1 $2",
        // Snake case (get_user_name → get user name)
        Regex("""_""") to " ",
        // Multiple dots (...)
        Regex("""\.\.\.""") to ", ",
        // Arrow notation
        Regex("""->""") to " to ",
        Regex("""=>""") to " maps to ",
        // Mathematical operators in context
        Regex("""!==""") to " is not strictly equal to ",
        Regex("""===""") to " is strictly equal to ",
        Regex("""!=""") to " is not equal to ",
        Regex("""==""") to " equals ",
        Regex("""<=""") to " less than or equal to ",
        Regex(""">=""") to " greater than or equal to ",
        // Hashtags
        Regex("""#(\w+)""") to "hashtag $1",
        // @mentions
        Regex("""@(\w+)""") to "at $1"
    )
    
    /**
     * Analyze document content and detect the most likely context.
     * Call this once when loading a document.
     */
    fun detectContext(fullDocumentText: String): DocumentContext {
        val lowerText = fullDocumentText.lowercase()
        
        var techScore = 0
        var medicalScore = 0
        
        techIndicators.forEach { indicator ->
            if (lowerText.contains(indicator)) techScore++
        }
        
        medicalIndicators.forEach { indicator ->
            if (lowerText.contains(indicator)) medicalScore++
        }
        
        currentContext = when {
            techScore > medicalScore && techScore >= 2 -> DocumentContext.TECHNICAL
            medicalScore > techScore && medicalScore >= 2 -> DocumentContext.MEDICAL
            else -> DocumentContext.GENERAL
        }
        
        return currentContext
    }
    
    /**
     * Set context manually (override auto-detection).
     */
    fun setContext(context: DocumentContext) {
        currentContext = context
    }
    
    /**
     * Preprocess text for natural TTS pronunciation.
     * Expands abbreviations, handles technical terms, and improves readability.
     */
    fun preprocess(text: String): String {
        var result = text
        
        // Apply pattern replacements first
        patternReplacements.forEach { (pattern, replacement) ->
            result = result.replace(pattern, replacement)
        }
        
        // Handle context-sensitive abbreviations
        contextAbbreviations.forEach { (abbr, meanings) ->
            val replacement = meanings[currentContext] ?: meanings[DocumentContext.GENERAL] ?: abbr
            // Match whole words only (case-insensitive)
            result = result.replace(
                Regex("""\b${Regex.escape(abbr)}\b""", RegexOption.IGNORE_CASE),
                replacement
            )
        }
        
        // Handle universal abbreviations
        universalAbbreviations.forEach { (abbr, expansion) ->
            result = result.replace(
                Regex("""\b${Regex.escape(abbr)}\b""", RegexOption.IGNORE_CASE),
                expansion
            )
        }
        
        // Handle numbers with units (e.g., "5GB" → "5 gigabytes")
        result = result.replace(Regex("""(\d+)\s*(GB|MB|KB|TB)""", RegexOption.IGNORE_CASE)) { match ->
            val number = match.groupValues[1]
            val unit = match.groupValues[2].lowercase()
            val unitExpansion = universalAbbreviations[unit] ?: unit
            "$number $unitExpansion"
        }
        
        // Clean up multiple spaces
        result = result.replace(Regex("""\s+"""), " ").trim()
        
        return result
    }
    
    /**
     * Get current detected/set context.
     */
    fun getCurrentContext(): DocumentContext = currentContext
    
    /**
     * Get a human-readable description of the current context.
     */
    fun getContextDescription(): String = when (currentContext) {
        DocumentContext.TECHNICAL -> "Technical/Programming"
        DocumentContext.MEDICAL -> "Medical/Scientific"
        DocumentContext.GENERAL -> "General"
    }
}
