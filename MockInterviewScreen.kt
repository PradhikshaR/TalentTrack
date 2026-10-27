package com.mad.talenttrack.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mad.talenttrack.data.model.UserProfile
import com.mad.talenttrack.ui.theme.*
import com.mad.talenttrack.utils.AnswerEvaluationResult
import com.mad.talenttrack.utils.FinalMockInterviewSummary
import com.mad.talenttrack.viewmodel.InterviewStep
import com.mad.talenttrack.viewmodel.MockInterviewViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockInterviewScreen(
    onBack: () -> Unit,
    onNavigateToResume: () -> Unit = {},
    viewModel: MockInterviewViewModel = viewModel()
) {
    val context = LocalContext.current
    val step by viewModel.step
    val profile by viewModel.userProfile
    val isLoadingProfile by viewModel.isLoadingProfile

    // Speech Recognizer setup
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startSpeechListening(context, speechRecognizer, viewModel)
        } else {
            Toast.makeText(context, "Microphone permission denied. You can type your answer.", Toast.LENGTH_SHORT).show()
            viewModel.setListeningState(false, "Microphone permission denied. Type your answer below.")
        }
    }

    DisposableEffect(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            val sr = SpeechRecognizer.createSpeechRecognizer(context)
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    viewModel.setListeningState(true, "Listening... Speak your answer now.")
                }

                override fun onBeginningOfSpeech() {
                    viewModel.setListeningState(true, "Listening to your answer...")
                }

                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    viewModel.setListeningState(false, "Processing speech to text...")
                }

                override fun onError(error: Int) {
                    viewModel.setListeningState(false)
                    val errorMsg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Try speaking clearly or type below."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout. Tap mic again or type your answer."
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. You can type your answer."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission required for voice recording."
                        else -> "Speech recognition error. Type your answer below."
                    }
                    viewModel.setListeningState(false, errorMsg)
                }

                override fun onResults(results: Bundle?) {
                    viewModel.setListeningState(false)
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        viewModel.appendSpeechText(matches[0])
                    } else {
                        viewModel.setListeningState(false, "No speech captured. You can type your answer below.")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        viewModel.setListeningState(true, "Recognizing: \"${matches[0]}\"")
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            speechRecognizer = sr
        }

        onDispose {
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (step == InterviewStep.COMPLETED) "Interview Results" else "Final Mock Interview",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoadingProfile) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentBlue)
                }
            } else {
                when (step) {
                    InterviewStep.START_SCREEN -> {
                        MockInterviewStartScreen(
                            profile = profile,
                            errorMessage = viewModel.errorMessage.value,
                            onStartInterview = { viewModel.startInterview() },
                            onGoToResume = onNavigateToResume
                        )
                    }
                    InterviewStep.LOADING_QUESTIONS -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = AccentBlue)
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Preparing personalized HR, technical & coding questions...",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    InterviewStep.INTERVIEWING, InterviewStep.EVALUATING_ANSWER, InterviewStep.SHOWING_EVALUATION -> {
                        MockInterviewerScreen(
                            viewModel = viewModel,
                            speechRecognizer = speechRecognizer,
                            permissionLauncher = permissionLauncher
                        )
                    }
                    InterviewStep.FINISHING_INTERVIEW -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = AccentGreen)
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Generating comprehensive feedback & saving result...",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    InterviewStep.COMPLETED -> {
                        val summary = viewModel.finalSummary.value
                        if (summary != null) {
                            MockInterviewResultScreen(
                                profile = profile,
                                summary = summary,
                                onDashboard = onBack,
                                onRetake = { viewModel.restartSession() }
                            )
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No interview results found.", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun startSpeechListening(
    context: Context,
    speechRecognizer: SpeechRecognizer?,
    viewModel: MockInterviewViewModel
) {
    if (speechRecognizer == null || !SpeechRecognizer.isRecognitionAvailable(context)) {
        Toast.makeText(context, "Speech recognition is not available on this device. Please type your answer.", Toast.LENGTH_SHORT).show()
        viewModel.setListeningState(false, "Speech recognition unavailable. You can type your answer below.")
        return
    }

    try {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your interview answer...")
        }
        speechRecognizer.startListening(intent)
        viewModel.setListeningState(true, "Listening... Speak your answer now.")
    } catch (e: Exception) {
        viewModel.setListeningState(false, "Speech error: ${e.message}. You can type below.")
    }
}

// =========================================================
// 1. START SCREEN
// =========================================================

@Composable
fun MockInterviewStartScreen(
    profile: UserProfile?,
    errorMessage: String?,
    onStartInterview: () -> Unit,
    onGoToResume: () -> Unit
) {
    val isAnalyzed = profile?.isAnalyzed == true && profile.targetJob.roleName.isNotBlank()
    val company = profile?.targetJob?.companyName?.ifBlank { "Selected Company" } ?: "Selected Company"
    val role = profile?.targetJob?.roleName?.ifBlank { "Selected Job Role" } ?: "Selected Job Role"
    val missingSkills = profile?.missingSkills?.filter { it.isNotBlank() } ?: emptyList()
    val priorities = profile?.skillPriorities.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(AccentBlue.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Final Mock Interview", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Real HR, Technical & Coding Interview", color = TextSecondary, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))
                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Target Company", color = TextSecondary, fontSize = 11.sp)
                        Text(company, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Job Role", color = TextSecondary, fontSize = 11.sp)
                        Text(role, color = AccentBlue, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Experience a full interview session (15-25 questions) covering resume, JD, technical concepts, real coding problems, and HR behavioral rounds.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }

        // Details Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.QuestionAnswer,
                title = "15-25 Questions",
                subtitle = "Adaptive Stages",
                color = AccentPurple
            )
            InfoChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Code,
                title = "Real Coding",
                subtitle = "Solution & Logic",
                color = AccentOrange
            )
            InfoChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.GraphicEq,
                title = "Voice or Text",
                subtitle = "Speech & Code Input",
                color = AccentGreen
            )
        }

        // Missing Skills Target Box
        if (missingSkills.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Target Technical Gaps in this Session:", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        missingSkills.take(4).forEach { skill ->
                            val isHigh = priorities[skill]?.equals("High", ignoreCase = true) == true
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isHigh) AccentOrange.copy(alpha = 0.2f) else AccentBlue.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isHigh) AccentOrange else AccentBlue)
                            ) {
                                Text(
                                    text = skill,
                                    color = if (isHigh) AccentOrange else Color.White,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Warning / Error Card if not analyzed
        if (!isAnalyzed) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AccentOrange.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = AccentOrange)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Please complete Resume & Job Description Analysis before starting the Final Mock Interview.",
                            color = Color.White,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onGoToResume,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Go to Resume & JD Analysis", color = Color.White)
                    }
                }
            }
        }

        if (!errorMessage.isNullOrBlank()) {
            Text(errorMessage, color = Color.Red, fontSize = 12.sp)
        }

        Spacer(Modifier.weight(1f))

        // Start Interview Button
        Button(
            onClick = onStartInterview,
            enabled = isAnalyzed,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Start Final Mock Interview", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun InfoChip(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(6.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center)
            Text(subtitle, color = TextSecondary, fontSize = 9.sp, textAlign = TextAlign.Center)
        }
    }
}

// =========================================================
// 2. INTERVIEWER SCREEN
// =========================================================

@Composable
fun MockInterviewerScreen(
    viewModel: MockInterviewViewModel,
    speechRecognizer: SpeechRecognizer?,
    permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
) {
    val context = LocalContext.current
    val currentIdx by viewModel.currentQuestionIndex
    val totalQuestions = viewModel.questions.size
    val currentQ = viewModel.questions.getOrNull(currentIdx)
    val step by viewModel.step

    val timerSeconds by viewModel.timerSeconds
    val isListening by viewModel.isListening
    val speechStatus by viewModel.speechStatus
    val transcript by viewModel.answerTranscript
    val currentEval by viewModel.currentEvaluation

    val isBlankAnswer = transcript.trim().isBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Progress & Top Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Question ${currentIdx + 1} of ${totalQuestions.coerceAtLeast(1)}",
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            // Timer Badge
            val timerColor = when {
                timerSeconds <= 15 -> Color.Red
                timerSeconds <= 30 -> AccentOrange
                else -> AccentGreen
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = timerColor.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, timerColor)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = timerColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    val formatTime = "${timerSeconds / 60}:${(timerSeconds % 60).toString().padStart(2, '0')}"
                    Text(
                        "$formatTime remaining",
                        color = timerColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        LinearProgressIndicator(
            progress = { (currentIdx + 1).toFloat() / totalQuestions.coerceAtLeast(1).toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = AccentBlue,
            trackColor = Color.DarkGray.copy(alpha = 0.3f)
        )

        // Interviewer Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(AccentBlue.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("TalentTrack Recruiter", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        val categoryText = currentQ?.category.orEmpty()
                        val skillText = currentQ?.relatedSkill.orEmpty()
                        val subtitleText = when {
                            categoryText.isNotBlank() && skillText.isNotBlank() -> "$categoryText • $skillText"
                            categoryText.isNotBlank() -> categoryText
                            else -> skillText
                        }
                        if (subtitleText.isNotBlank()) {
                            Text(subtitleText, color = TextSecondary, fontSize = 10.sp)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (currentQ?.isCodingQuestion == true) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AccentOrange.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentOrange),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Real Coding Challenge", color = AccentOrange, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                Text(
                    text = currentQ?.questionText ?: "Question...",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                )
            }
        }

        when (step) {
            InterviewStep.INTERVIEWING -> {
                // Speech & Mic Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackground)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Pulsing Mic Button
                        val pulseAnim = rememberInfiniteTransition(label = "pulse")
                        val scale by pulseAnim.animateFloat(
                            initialValue = 1f,
                            targetValue = if (isListening) 1.15f else 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ), label = "scale"
                        )

                        Box(
                            modifier = Modifier
                                .scale(scale)
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isListening) Brush.radialGradient(listOf(Color.Red, AccentOrange))
                                    else Brush.radialGradient(listOf(AccentBlue, AccentPurple))
                                )
                                .clickable {
                                    if (isListening) {
                                        speechRecognizer?.stopListening()
                                        viewModel.setListeningState(false, "Speech stopped. You can edit or submit below.")
                                    } else {
                                        val hasPermission = ContextCompat.checkSelfPermission(
                                            context, Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED

                                        if (hasPermission) {
                                            startSpeechListening(context, speechRecognizer, viewModel)
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = "Microphone",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = speechStatus,
                            color = if (isListening) AccentOrange else if (isBlankAnswer && speechStatus.contains("Please")) Color.Yellow else TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(14.dp))

                        // Answer Transcript / Code Input Field
                        val isCoding = currentQ?.isCodingQuestion == true
                        OutlinedTextField(
                            value = transcript,
                            onValueChange = { viewModel.updateAnswerTranscript(it) },
                            placeholder = {
                                Text(
                                    text = if (isCoding) "Write your code/query solution here and briefly explain your logic..." else "Speak using mic or type your detailed answer here...",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    fontFamily = if (isCoding) FontFamily.Monospace else FontFamily.Default
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isCoding) 180.dp else 130.dp),
                            shape = RoundedCornerShape(14.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = if (isCoding) FontFamily.Monospace else FontFamily.Default,
                                fontSize = 13.sp
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = if (isBlankAnswer) Color.DarkGray else AccentBlue.copy(alpha = 0.5f),
                                focusedBorderColor = AccentBlue,
                                unfocusedTextColor = Color.White,
                                focusedTextColor = Color.White,
                                focusedContainerColor = DarkBackground.copy(alpha = 0.5f),
                                unfocusedContainerColor = DarkBackground.copy(alpha = 0.5f)
                            )
                        )

                        Spacer(Modifier.height(12.dp))

                        // Action buttons: Retake, Submit Answer, End Interview
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.retakeAnswer() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Retake", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    if (isBlankAnswer) {
                                        viewModel.setListeningState(false, "Please enter your answer before continuing.")
                                    } else {
                                        viewModel.submitAnswer()
                                    }
                                },
                                enabled = !isBlankAnswer,
                                modifier = Modifier.weight(2f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentBlue,
                                    disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                                )
                            ) {
                                Text("Submit Answer", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        TextButton(
                            onClick = { viewModel.finishInterview() },
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text("End Interview Early", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
            }
            InterviewStep.EVALUATING_ANSWER -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AccentBlue)
                        Spacer(Modifier.height(12.dp))
                        Text("Evaluating your answer...", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
            InterviewStep.SHOWING_EVALUATION -> {
                if (currentEval != null) {
                    AnswerEvaluationCard(
                        evaluation = currentEval!!,
                        isCoding = currentQ?.isCodingQuestion == true,
                        onNext = { viewModel.nextQuestion() },
                        isLast = currentIdx >= totalQuestions - 1
                    )
                }
            }
            else -> {}
        }
    }
}

// =========================================================
// 3. ANSWER EVALUATION CARD
// =========================================================

@Composable
fun AnswerEvaluationCard(
    evaluation: AnswerEvaluationResult,
    isCoding: Boolean,
    onNext: () -> Unit,
    isLast: Boolean
) {
    val isPoorAnswer = evaluation.relevanceScore < 3 || evaluation.technicalScore < 3

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Answer Evaluation", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)

            // Scores Grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ScoreBadge("Relevance", evaluation.relevanceScore)
                ScoreBadge("Clarity", evaluation.clarityScore)
                ScoreBadge("Technical", evaluation.technicalScore)
                ScoreBadge("Communication", evaluation.communicationScore)
                if (isCoding || evaluation.codingScore > 0) {
                    ScoreBadge("Coding Logic", evaluation.codingScore)
                }
            }

            HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))

            if (evaluation.strengths.isNotEmpty()) {
                Text(
                    text = if (isPoorAnswer) "Observation:" else "What went well:",
                    color = if (isPoorAnswer) AccentOrange else AccentGreen,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
                evaluation.strengths.forEach { s ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isPoorAnswer) Icons.Default.Info else Icons.Default.Check,
                            contentDescription = null,
                            tint = if (isPoorAnswer) AccentOrange else AccentGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(s, color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            if (evaluation.improvements.isNotEmpty()) {
                Text("Areas for improvement:", color = AccentOrange, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                evaluation.improvements.forEach { imp ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.ArrowRight, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(imp, color = Color.White, fontSize = 12.sp)
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = AccentBlue.copy(alpha = 0.1f)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(evaluation.shortSuggestion, color = Color.White, fontSize = 11.sp, lineHeight = 16.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text(if (isLast) "Finish & View Results" else "Next Question >", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun ScoreBadge(label: String, score: Int) {
    val color = when {
        score >= 7 -> AccentGreen
        score >= 4 -> AccentOrange
        else -> Color.Red
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$score/10", color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(label, color = TextSecondary, fontSize = 10.sp)
    }
}

// =========================================================
// 4. FINAL RESULT SCREEN
// =========================================================

@Composable
fun MockInterviewResultScreen(
    profile: UserProfile?,
    summary: FinalMockInterviewSummary,
    onDashboard: () -> Unit,
    onRetake: () -> Unit
) {
    val role = profile?.targetJob?.roleName?.ifBlank { "Software Engineer" } ?: "Software Engineer"
    val company = profile?.targetJob?.companyName?.ifBlank { "Target Company" } ?: "Target Company"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header Ring
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(8.dp))
                Text("FINAL MOCK INTERVIEW RESULT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("$role at $company", color = TextSecondary, fontSize = 12.sp)

                Spacer(Modifier.height(20.dp))

                // Score Display Box
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(AccentBlue.copy(alpha = 0.15f), CircleShape)
                        .border(4.dp, AccentBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${summary.overallScore}%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 32.sp)
                        Text("Overall Score", color = TextSecondary, fontSize = 10.sp)
                    }
                }
            }
        }

        // Category Breakdown Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Performance Breakdown", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(12.dp))

                summary.categoryScores.forEach { (cat, valScore) ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(cat, color = TextSecondary, fontSize = 12.sp)
                            Text("$valScore%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { valScore / 100f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = if (valScore >= 75) AccentGreen else AccentOrange,
                            trackColor = Color.DarkGray.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }

        // Strong & Needs Improvement
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ThumbUp, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Strong Areas", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    summary.strongAreas.forEach { area ->
                        Text("• $area", color = Color.White, fontSize = 11.sp, lineHeight = 15.sp)
                    }
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Build, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Needs Focus", color = AccentOrange, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    summary.needsImprovement.forEach { area ->
                        Text("• $area", color = Color.White, fontSize = 11.sp, lineHeight = 15.sp)
                    }
                }
            }
        }

        // Coding Areas To Improve Card
        if (summary.codingAreasToImprove.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = AccentOrange, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Coding Areas To Improve:", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    summary.codingAreasToImprove.forEach { item ->
                        Text("• $item", color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
                    }
                }
            }
        }

        // Top Skills To Revise Card
        if (summary.topSkillsToRevise.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Top Skills To Revise:", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        summary.topSkillsToRevise.forEach { skill ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = AccentBlue.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue)
                            ) {
                                Text(
                                    text = skill,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Final Feedback Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Recruiter Final Feedback", color = AccentBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Text(summary.finalFeedback, color = Color.White, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Retake Interview")
            }

            Button(
                onClick = onDashboard,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text("Return to Dashboard")
            }
        }
    }
}
