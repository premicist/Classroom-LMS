package com.aistudio.classroomlms.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

val CalmPrimary = Color(0xFF263B50)
val CalmOnPrimary = Color(0xFFFFFFFF)
val CalmPrimaryContainer = Color(0xFFDCE5EB)
val CalmOnPrimaryContainer = Color(0xFF172B3B)

val CalmSecondary = Color(0xFF896A32)
val CalmOnSecondary = Color(0xFFFFFFFF)
val CalmSecondaryContainer = Color(0xFFF0E5CA)
val CalmOnSecondaryContainer = Color(0xFF3F2F12)

val CalmTertiary = Color(0xFF526F70)
val CalmOnTertiary = Color(0xFFFFFFFF)
val CalmTertiaryContainer = Color(0xFFD6E7E4)
val CalmOnTertiaryContainer = Color(0xFF203838)

val CalmBackgroundLight = Color(0xFFF6F4EF)
val CalmSurfaceLight = Color(0xFFFFFEFB)
val CalmSurfaceContainerLight = Color(0xFFEDEAE3)
val CalmSurfaceVariantLight = Color(0xFFE8E5DD)
val CalmBorderLight = Color(0xFF77776F)
val CalmBorderSubtleLight = Color(0xFFD6D2C9)
val CalmTextPrimaryLight = Color(0xFF202A32)
val CalmTextSecondaryLight = Color(0xFF4E5960)

val CalmPrimaryDark = Color(0xFFB9C9D7)
val CalmOnPrimaryDark = Color(0xFF203548)
val CalmPrimaryContainerDark = Color(0xFF394F63)
val CalmOnPrimaryContainerDark = Color(0xFFD7E3EC)

val CalmSecondaryDark = Color(0xFFE2C88E)
val CalmOnSecondaryDark = Color(0xFF392B10)
val CalmSecondaryContainerDark = Color(0xFF57451F)
val CalmOnSecondaryContainerDark = Color(0xFFF3E4C1)

val CalmTertiaryDark = Color(0xFFB9D1CE)
val CalmOnTertiaryDark = Color(0xFF243A39)
val CalmTertiaryContainerDark = Color(0xFF3A5352)
val CalmOnTertiaryContainerDark = Color(0xFFD5E7E3)

val CalmBackgroundDark = Color(0xFF171C20)
val CalmSurfaceDark = Color(0xFF1E2428)
val CalmSurfaceContainerDark = Color(0xFF252C30)
val CalmSurfaceVariantDark = Color(0xFF41494D)
val CalmBorderDark = Color(0xFF929B9F)
val CalmBorderSubtleDark = Color(0xFF41494D)
val CalmTextPrimaryDark = Color(0xFFE2E7E8)
val CalmTextSecondaryDark = Color(0xFFC1C9CB)

val StatusSuccess = Color(0xFF356847)
val StatusSuccessBg = Color(0xFFDCEBDD)
val StatusSuccessDark = Color(0xFFA5D2AE)
val StatusSuccessBgDark = Color(0xFF263D2D)
val StatusSuccessOnDark = Color(0xFF203728)

val StatusWarning = Color(0xFF795A1C)
val StatusWarningBg = Color(0xFFF0E5C9)
val StatusWarningDark = Color(0xFFE6C77E)
val StatusWarningBgDark = Color(0xFF453919)
val StatusWarningOnDark = Color(0xFF3C2D0D)

val StatusError = Color(0xFFB3261E)
val StatusErrorBg = Color(0xFFF4DEDB)
val StatusErrorDark = Color(0xFFFFB4AB)
val StatusErrorBgDark = Color(0xFF5B2521)

val StatusInfo = Color(0xFF3C667D)
val StatusInfoBg = Color(0xFFDCE8ED)
val StatusInfoDark = Color(0xFFA8CADA)
val StatusInfoBgDark = Color(0xFF263C48)

val StatusNeutral = Color(0xFF596166)
val StatusNeutralBg = Color(0xFFE6E5E0)
val StatusNeutralDark = Color(0xFFC2C8C9)
val StatusNeutralBgDark = Color(0xFF353B3F)

val StatusEnrichment = Color(0xFF5E607F)
val StatusEnrichmentBg = Color(0xFFE5E2EE)
val StatusEnrichmentDark = Color(0xFFC8C3E0)
val StatusEnrichmentBgDark = Color(0xFF38364D)
val StatusEnrichmentOnDark = Color(0xFF302E43)

val SubjectMath = Color(0xFF334F69)
val SubjectScience = Color(0xFF426E6B)
val SubjectHistory = Color(0xFF896A32)
val SubjectEnglish = Color(0xFF685B70)
val SubjectArt = Color(0xFF9A5B49)
val SubjectTech = Color(0xFF526F8A)

val VibrantPrimary = CalmPrimary
val VibrantOnPrimary = CalmOnPrimary
val VibrantPrimaryContainer = CalmPrimaryContainer
val VibrantOnPrimaryContainer = CalmOnPrimaryContainer
val VibrantSecondary = CalmSecondary
val VibrantOnSecondary = CalmOnSecondary
val VibrantSecondaryContainer = CalmSecondaryContainer
val VibrantOnSecondaryContainer = CalmOnSecondaryContainer
val VibrantTertiary = CalmTertiary
val VibrantOnTertiary = CalmOnTertiary
val VibrantTertiaryContainer = CalmTertiaryContainer
val VibrantOnTertiaryContainer = CalmOnTertiaryContainer
val VibrantBackgroundLight = CalmBackgroundLight
val VibrantSurfaceLight = CalmSurfaceLight
val VibrantSurfaceContainerLight = CalmSurfaceContainerLight
val VibrantSurfaceVariantLight = CalmSurfaceVariantLight
val VibrantBorderLight = CalmBorderLight
val VibrantBorderSubtleLight = CalmBorderSubtleLight
val VibrantBackgroundDark = CalmBackgroundDark
val VibrantSurfaceDark = CalmSurfaceDark
val VibrantSurfaceContainerDark = CalmSurfaceContainerDark
val VibrantSurfaceVariantDark = CalmSurfaceVariantDark
val VibrantBorderDark = CalmBorderDark
val VibrantBorderSubtleDark = CalmBorderSubtleDark
val VibrantTextPrimary = CalmTextPrimaryLight
val VibrantTextSecondary = CalmTextSecondaryLight

val EduPrimary = CalmPrimary
val EduPrimaryContainer = CalmPrimaryContainer
val EduOnPrimaryContainer = CalmOnPrimaryContainer
val EduSecondary = CalmSecondary
val EduBackground = CalmBackgroundLight
val EduSurface = CalmSurfaceLight
val EduBorder = CalmBorderLight
val EduBorderSubtle = CalmBorderSubtleLight
val EduTextPrimary = CalmTextPrimaryLight
val EduTextSecondary = CalmTextSecondaryLight

@Composable
private fun ColorScheme.usesDarkSurfaces(): Boolean = surface.luminance() < 0.5f

val ColorScheme.statusSuccess: Color
    @Composable get() = if (usesDarkSurfaces()) StatusSuccessDark else StatusSuccess

val ColorScheme.onStatusSuccess: Color
    @Composable get() = if (usesDarkSurfaces()) StatusSuccessOnDark else Color.White

val ColorScheme.statusSuccessContainer: Color
    @Composable get() = if (usesDarkSurfaces()) StatusSuccessBgDark else StatusSuccessBg

val ColorScheme.statusWarning: Color
    @Composable get() = if (usesDarkSurfaces()) StatusWarningDark else StatusWarning

val ColorScheme.onStatusWarning: Color
    @Composable get() = if (usesDarkSurfaces()) StatusWarningOnDark else Color.White

val ColorScheme.statusWarningContainer: Color
    @Composable get() = if (usesDarkSurfaces()) StatusWarningBgDark else StatusWarningBg

val ColorScheme.statusInfo: Color
    @Composable get() = if (usesDarkSurfaces()) StatusInfoDark else StatusInfo

val ColorScheme.statusInfoContainer: Color
    @Composable get() = if (usesDarkSurfaces()) StatusInfoBgDark else StatusInfoBg

val ColorScheme.statusEnrichment: Color
    @Composable get() = if (usesDarkSurfaces()) StatusEnrichmentDark else StatusEnrichment

val ColorScheme.onStatusEnrichment: Color
    @Composable get() = if (usesDarkSurfaces()) StatusEnrichmentOnDark else Color.White

val ColorScheme.statusEnrichmentContainer: Color
    @Composable get() = if (usesDarkSurfaces()) StatusEnrichmentBgDark else StatusEnrichmentBg
