import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await // Import this for .await()
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// 🔑 Rename the class to DailyReport
class DailyReport(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    // 🔑 Override the doWork() function
    override suspend fun doWork(): Result {

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = dateFormat.format(Date())

        val reportsRef = FirebaseFirestore.getInstance().collection("daily_reports")

        val dataToSend = hashMapOf(
            "date" to date,
            "total_waste_kg" to 0,
            "upload_successful" to true
        )

        try {
            reportsRef.document(date).set(dataToSend)
                .await()

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}