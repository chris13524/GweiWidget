package xyz.smith.gweiwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Implementation of App Widget functionality.
 */
class GweiWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val etherscanApi = RetrofitClient.instance.create(EtherscanApi::class.java)
    val call = etherscanApi.getGasOracle("gastracker", "gasoracle", apiKey)

    call.enqueue(object : Callback<GasOracleResponse> {
        override fun onResponse(call: Call<GasOracleResponse>, response: Response<GasOracleResponse>) {
            if (response.isSuccessful) {
                val proposeGasPrice = response.body()?.result?.ProposeGasPrice

                val gwei = proposeGasPrice
                val widgetText = "${gwei} gwei"
                // Construct the RemoteViews object
                val views = RemoteViews(context.packageName, R.layout.gwei_widget)
                views.setTextViewText(R.id.appwidget_text, widgetText)

                // Instruct the widget manager to update the widget
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } else {
                error(response)
            }
        }

        override fun onFailure(call: Call<GasOracleResponse>, t: Throwable) {
            error(t)
        }
    })
}

data class GasOracleResponse(
    val status: String,
    val message: String,
    val result: Result
)

data class Result(
    val LastBlock: String,
    val SafeGasPrice: String,
    val ProposeGasPrice: String,
    val FastGasPrice: String,
    val suggestBaseFee: String,
    val gasUsedRatio: String
)

interface EtherscanApi {
    @GET("api")
    fun getGasOracle(
        @Query("module") module: String,
        @Query("action") action: String,
        @Query("apikey") apikey: String
    ): Call<GasOracleResponse>
}


object RetrofitClient {
    private const val BASE_URL = "https://api.etherscan.io/"

    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}