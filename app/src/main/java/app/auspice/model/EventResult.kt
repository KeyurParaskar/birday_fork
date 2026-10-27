package app.auspice.model

import java.io.Serializable
import java.time.LocalDate

data class EventResult (
    val id: Int,
    val type: String? = EventCode.BIRTHDAY.name,
    var name: String,
    var surname: String? = "",
    var favorite: Boolean? = false,
    val yearMatter: Boolean? = true,
    var originalDate: LocalDate,
    val nextDate: LocalDate? = null,
    var notes: String? = "",
    val image: ByteArray? = null,
    val sex: String? = null,
    // Optional birth time ("HH:mm"), place (label, coordinates, IANA zone) and personality type ("INTJ-A")
    val birthTime: String? = null,
    val birthPlace: String? = null,
    val birthLatitude: Double? = null,
    val birthLongitude: Double? = null,
    val birthZone: String? = null,
    var personality: String? = null,
): Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EventResult

        if (id != other.id) return false
        if (type != other.type) return false
        if (name != other.name) return false
        if (surname != other.surname) return false
        if (yearMatter != other.yearMatter) return false
        if (originalDate != other.originalDate) return false
        if (!image.contentEquals(other.image)) return false
        if (sex != other.sex) return false
        if (birthTime != other.birthTime) return false
        if (birthPlace != other.birthPlace) return false
        if (birthLatitude != other.birthLatitude) return false
        if (birthLongitude != other.birthLongitude) return false
        if (birthZone != other.birthZone) return false
        if (personality != other.personality) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + (type?.hashCode() ?: 0)
        result = 31 * result + name.hashCode()
        result = 31 * result + (surname?.hashCode() ?: 0)
        result = 31 * result + (favorite?.hashCode() ?: 0)
        result = 31 * result + (yearMatter?.hashCode() ?: 0)
        result = 31 * result + originalDate.hashCode()
        result = 31 * result + (nextDate?.hashCode() ?: 0)
        result = 31 * result + (notes?.hashCode() ?: 0)
        result = 31 * result + (image?.contentHashCode() ?: 0)
        result = 31 * result + (sex?.hashCode() ?: 0)
        result = 31 * result + (birthTime?.hashCode() ?: 0)
        result = 31 * result + (birthPlace?.hashCode() ?: 0)
        result = 31 * result + (birthLatitude?.hashCode() ?: 0)
        result = 31 * result + (birthLongitude?.hashCode() ?: 0)
        result = 31 * result + (birthZone?.hashCode() ?: 0)
        result = 31 * result + (personality?.hashCode() ?: 0)
        return result
    }
}