package piuk.blockchain.android.data

interface Mapper<in A, out B> {
    fun map(type: A): B
}