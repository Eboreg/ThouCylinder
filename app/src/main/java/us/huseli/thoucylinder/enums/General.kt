package us.huseli.thoucylinder.enums

import android.content.Context
import androidx.annotation.StringRes
import us.huseli.thoucylinder.R

enum class AlbumType(@StringRes val stringRes: Int) {
    ALBUM(R.string.album),
    EP(R.string.ep),
    SINGLE(R.string.single),
    COMPILATION(R.string.compilation),
}

enum class AvailabilityFilter(@StringRes val stringRes: Int) {
    ALL(R.string.all),
    ONLY_PLAYABLE(R.string.only_playable),
    ONLY_LOCAL(R.string.only_local),
}

enum class ListUpdateStrategy { MERGE, REPLACE }

enum class PlaybackState { STOPPED, PLAYING, PAUSED }

enum class RadioStatus { INACTIVE, LOADING, LOADING_MORE, LOADED }

enum class RadioType(@StringRes val stringRes: Int) {
    LIBRARY(R.string.library),
    ARTIST(R.string.artist),
    ALBUM(R.string.album),
    TRACK(R.string.track),
}

enum class Region(@StringRes val stringRes: Int) {
    DZ(R.string.algeria),
    AR(R.string.argentina),
    AU(R.string.australia),
    AT(R.string.austria),
    AZ(R.string.azerbaijan),
    BH(R.string.bahrain),
    BY(R.string.belarus),
    BE(R.string.belgium),
    BA(R.string.bosnia_and_herzegovina),
    BR(R.string.brazil),
    BG(R.string.bulgaria),
    CA(R.string.canada),
    CL(R.string.chile),
    CO(R.string.colombia),
    HR(R.string.croatia),
    CZ(R.string.czech_republic),
    DK(R.string.denmark),
    EG(R.string.egypt),
    EE(R.string.estonia),
    FI(R.string.finland),
    FR(R.string.france),
    GE(R.string.georgia),
    DE(R.string.germany),
    GH(R.string.ghana),
    GR(R.string.greece),
    HK(R.string.hong_kong),
    HU(R.string.hungary),
    IS(R.string.iceland),
    IN(R.string.india),
    ID(R.string.indonesia),
    IQ(R.string.iraq),
    IE(R.string.ireland),
    IL(R.string.israel),
    IT(R.string.italy),
    JM(R.string.jamaica),
    JP(R.string.japan),
    JO(R.string.jordan),
    KZ(R.string.kazakhstan),
    KE(R.string.kenya),
    KW(R.string.kuwait),
    LV(R.string.latvia),
    LB(R.string.lebanon),
    LY(R.string.libya),
    LT(R.string.lithuania),
    LU(R.string.luxembourg),
    MK(R.string.macedonia),
    MY(R.string.malaysia),
    MX(R.string.mexico),
    ME(R.string.montenegro),
    MA(R.string.morocco),
    NP(R.string.nepal),
    NL(R.string.netherlands),
    NZ(R.string.new_zealand),
    NG(R.string.nigeria),
    NO(R.string.norway),
    OM(R.string.oman),
    PK(R.string.pakistan),
    PE(R.string.peru),
    PH(R.string.philippines),
    PL(R.string.poland),
    PT(R.string.portugal),
    PR(R.string.puerto_rico),
    QA(R.string.qatar),
    RO(R.string.romania),
    RU(R.string.russia),
    SA(R.string.saudi_arabia),
    SN(R.string.senegal),
    RS(R.string.serbia),
    SG(R.string.singapore),
    SK(R.string.slovakia),
    SI(R.string.slovenia),
    ZA(R.string.south_africa),
    KR(R.string.south_korea),
    ES(R.string.spain),
    LK(R.string.sri_lanka),
    SE(R.string.sweden),
    CH(R.string.switzerland),
    TW(R.string.taiwan),
    TZ(R.string.tanzania),
    TH(R.string.thailand),
    TN(R.string.tunisia),
    TR(R.string.turkey),
    UG(R.string.uganda),
    UA(R.string.ukraine),
    AE(R.string.united_arab_emirates),
    GB(R.string.united_kingdom),
    US(R.string.united_states),
    VN(R.string.vietnam),
    YE(R.string.yemen),
    ZW(R.string.zimbabwe);

    companion object {
        fun filteredEntries(context: Context, filter: String?): List<Region> =
            if (filter != null) entries.filter { context.getString(it.stringRes).contains(filter, true) }
            else entries
    }
}
