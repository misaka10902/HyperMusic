package com.hyper.music.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyper.music.R
import com.hyper.music.ui.effects.miuix.cardGlass
import com.hyper.music.ui.effects.miuix.cardGlassColors
import com.hyper.music.ui.utils.horizontalContentPadding
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
    }
}

@Composable
fun AboutAppScreen(onBack: () -> Unit, backdrop: LayerBackdrop) {
    val context = LocalContext.current

    @Suppress("DEPRECATION")
    val versionName = rememberSafeVersion(context)
    val horizontalPadding = horizontalContentPadding()

    DetailPage(
        title = "关于",
        onBack = onBack,
        backdrop = backdrop,
        header = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp)),
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "澎湃音乐",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "版本 $versionName",
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                )
            }
        },
        list = {
            item {


                Box(Modifier.padding(horizontal = horizontalPadding)) {
                    AboutSection(title = "项目") {
                        ArrowPreference(
                            title = "开源许可",
                            summary = "GNU AGPL-3.0",
                            onClick = {
                                openUrl(context, "https://www.gnu.org/licenses/agpl-3.0.en.html")
                            },
                        )
                        ArrowPreference(
                            title = "项目主页",
                            onClick = {
                                openUrl(context, "https://github.com/misaka10902/HyperMusic")
                            },
                        )
                    }
                }
            }
            item {
                Spacer(Modifier.height(12.dp))
                Box(Modifier.padding(horizontal = horizontalPadding)) {
                    AboutSection(title = "致谢") {
                        ArrowPreference(
                            title = "GD Studio",
                            summary = "音源服务提供",
                            onClick = {
                                openUrl(context, "https://music.gdstudio.org/")
                            },
                        )
                        ArrowPreference(
                            title = "Otter Music",
                            summary = "提供API接入思路",
                            onClick = {
                                openUrl(context, "https://otter-music.pages.dev/")
                            },
                        )
                        ArrowPreference(
                            title = "Compose Miuix",
                            summary = "界面基础框架，澎湃化的根基",
                            onClick = {
                                openUrl(context, "https://compose-miuix-ui.github.io/miuix/")
                            },
                        )
                        ArrowPreference(
                            title = "AndroidX Media3",
                            summary = "音频播放基础框架",
                            onClick = {
                                openUrl(context, "https://developer.android.com/media/media3")
                            },
                        )
                        ArrowPreference(
                            title = "Nexio Scheduler",
                            summary = "项目初期界面构建参考",
                            onClick = {
                                openUrl(context, "https://haozai000.github.io/NexioSchedule/")
                            },
                        )
                        ArrowPreference(
                            title = "Melox",
                            summary = "UX细节方案提供",
                            onClick = {
                                openUrl(context, "https://github.com/Inefy-03/Melox")
                            },
                        )
                    }
                }
            }
        },
    )
}


@Composable
private fun AboutSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SmallTitle(text = title, modifier = Modifier.offset(x = (-15).dp))
        Card(
            cornerRadius = 20.dp,
            modifier = Modifier
                .fillMaxWidth()
                .cardGlass(RoundedCornerShape(20.dp)),
            insideMargin = PaddingValues(0.dp),
            colors = cardGlassColors(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content,
            )
        }
    }
}

@Composable
private fun rememberSafeVersion(context: Context): String {
    return androidx.compose.runtime.remember {
        try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }
}
