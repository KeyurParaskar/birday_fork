package app.auspice.fragments.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import app.auspice.R
import app.auspice.databinding.BottomSheetPersonalityTestBinding
import com.auspice.cosmos.PersonalityTest
import com.auspice.cosmos.PersonalityType
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * Runs the full personality test (50 statements) or the fifth-letter test (10 statements, needs the first
 * four letters) for one person, then shows the result and hands the code ("INTJ-A") to [onSaved].
 */
class PersonalityTestBottomSheet(
    private val personName: String,
    private val fifthLetterOnly: Boolean,
    private val knownLetters: String?,
    private val onSaved: (String) -> Unit,
) : BottomSheetDialogFragment() {
    private var _binding: BottomSheetPersonalityTestBinding? = null
    private val binding get() = _binding!!

    private val questions = if (fifthLetterOnly) PersonalityTest.IDENTITY_ONLY else PersonalityTest.FULL
    // 0 means not answered yet, otherwise 1 (strongly disagree) to 5 (strongly agree)
    private val answers = IntArray(questions.size)
    private var index = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetPersonalityTestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private val radioIds by lazy {
        listOf(
            binding.testAnswer1.id,
            binding.testAnswer2.id,
            binding.testAnswer3.id,
            binding.testAnswer4.id,
            binding.testAnswer5.id,
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (dialog as BottomSheetDialog).behavior.state = BottomSheetBehavior.STATE_EXPANDED
        binding.testTitle.text = getString(
            if (fifthLetterOnly) R.string.personality_fifth_letter_title else R.string.personality_test_title,
            personName
        )
        binding.testProgress.max = questions.size

        binding.testAnswers.setOnCheckedChangeListener { _, checkedId ->
            val position = radioIds.indexOf(checkedId)
            // Clearing the group between questions reports -1; that is not an answer
            if (position >= 0) {
                answers[index] = position + 1
                binding.testNext.isEnabled = true
            }
        }
        binding.testNext.setOnClickListener {
            if (answers[index] == 0) return@setOnClickListener
            if (index < questions.lastIndex) {
                index++
                showQuestion()
            } else showResult()
        }
        binding.testBack.setOnClickListener {
            if (index > 0) {
                index--
                showQuestion()
            }
        }
        showQuestion()
    }

    private fun showQuestion() {
        binding.testQuestion.text = questions[index].text
        binding.testProgress.setProgressCompat(index + 1, true)
        binding.testProgressText.text = getString(R.string.test_progress, index + 1, questions.size)
        binding.testAnswers.clearCheck()
        if (answers[index] > 0) binding.testAnswers.check(radioIds[answers[index] - 1])
        binding.testNext.isEnabled = answers[index] > 0
        binding.testNext.text = getString(if (index == questions.lastIndex) R.string.test_see_result else R.string.test_next)
        binding.testBack.isEnabled = index > 0
    }

    private fun showResult() {
        val result = PersonalityTest.score(questions, answers.toList(), knownLetters)
        val type = result.type
        binding.testQuestionGroup.visibility = View.GONE
        binding.testResultGroup.visibility = View.VISIBLE
        binding.testResultType.text = type.code
        val lines = mutableListOf(getString(R.string.personality_result_nickname, type.nickname), type.summary, "")
        result.preferences.forEach {
            val name = if (it.letter == it.dimension.first) it.dimension.firstName else it.dimension.secondName
            lines += getString(R.string.personality_result_strength, name, it.percent)
        }
        type.identity?.let { lines += "\n" + PersonalityType.identitySummary(it) }
        binding.testResultDetails.text = lines.joinToString("\n")
        binding.testSave.setOnClickListener {
            onSaved(type.code)
            dismiss()
        }
    }
}
