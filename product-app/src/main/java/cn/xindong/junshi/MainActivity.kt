package cn.xindong.junshi

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

class MainActivity: ComponentActivity() {
    private var sharedText by mutableStateOf<String?>(null)
    private var reviewRequest by mutableIntStateOf(0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); enableEdgeToEdge(); receive(intent)
        setContent {
            JunshiTheme {
                Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
                    ProductScreen((application as JunshiApp).controller,sharedText,reviewRequest) { sharedText=null }
                }
            }
        }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); receive(intent) }
    private fun receive(intent: Intent?) {
        if(intent?.action==Intent.ACTION_SEND && intent.type=="text/plain") sharedText=intent.getStringExtra(Intent.EXTRA_TEXT)
        if(intent?.getBooleanExtra("review_latest",false)==true) { reviewRequest++; intent.removeExtra("review_latest") }
    }
}

