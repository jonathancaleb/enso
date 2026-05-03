package com.example.enso.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.example.enso.R

val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val dmSansFont = GoogleFont("DM Sans")
val dmSerifDisplayFont = GoogleFont("DM Serif Display")

val DmSansFamily = FontFamily(
    Font(googleFont = dmSansFont, fontProvider = googleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = dmSansFont, fontProvider = googleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = dmSansFont, fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = dmSansFont, fontProvider = googleFontProvider, weight = FontWeight.Bold),
)

val DmSerifFamily = FontFamily(
    Font(googleFont = dmSerifDisplayFont, fontProvider = googleFontProvider, weight = FontWeight.Normal),
)

val EnsoTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = DmSerifFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        color = EnsoOnBackground
    ),
    headlineLarge = TextStyle(
        fontFamily = DmSerifFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        color = EnsoOnBackground
    ),
    headlineMedium = TextStyle(
        fontFamily = DmSerifFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        color = EnsoOnBackground
    ),
    titleLarge = TextStyle(
        fontFamily = DmSerifFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        color = EnsoOnBackground
    ),
    titleMedium = TextStyle(
        fontFamily = DmSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = EnsoOnBackground
    ),
    titleSmall = TextStyle(
        fontFamily = DmSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = EnsoOnBackground
    ),
    bodyLarge = TextStyle(
        fontFamily = DmSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = EnsoOnBackground
    ),
    bodyMedium = TextStyle(
        fontFamily = DmSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = EnsoSecondaryText
    ),
    bodySmall = TextStyle(
        fontFamily = DmSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = EnsoSecondaryText
    ),
    labelLarge = TextStyle(
        fontFamily = DmSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = EnsoOnBackground
    ),
    labelMedium = TextStyle(
        fontFamily = DmSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = EnsoSecondaryText
    ),
    labelSmall = TextStyle(
        fontFamily = DmSansFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        color = EnsoSecondaryText
    )
)
