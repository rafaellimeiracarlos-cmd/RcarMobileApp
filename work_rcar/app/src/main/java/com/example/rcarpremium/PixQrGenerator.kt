package com.example.rcarpremium

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter

object PixQrGenerator {
    fun payload(key:String, amount:Double, merchantName:String, merchantCity:String, txid:String="***"):String {
        fun tlv(id:String,value:String)=id+value.toByteArray(Charsets.UTF_8).size.toString().padStart(2,'0')+value
        val gui=tlv("00","br.gov.bcb.pix")
        val keyField=tlv("01",key.trim())
        val merchantAccount=tlv("26",gui+keyField)
        val name=merchantName.trim().uppercase().take(25).ifBlank { "RCAR PREMIUM" }
        val city=merchantCity.trim().uppercase().take(15).ifBlank { "SAO PAULO" }
        val cents=String.format(java.util.Locale.US,"%.2f",amount.coerceAtLeast(0.01))
        val tx=tlv("62",tlv("05",txid.take(25)))
        val base=tlv("00","01")+tlv("01","11")+merchantAccount+tlv("52","0000")+tlv("53","986")+tlv("54",cents)+tlv("58","BR")+tlv("59",name)+tlv("60",city)+tx+"6304"
        return base+crc16(base).toString(16).uppercase().padStart(4,'0')
    }
    fun bitmap(payload:String,size:Int=720):Bitmap {
        val matrix=MultiFormatWriter().encode(payload,BarcodeFormat.QR_CODE,size,size)
        val bitmap=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888)
        for(x in 0 until size) for(y in 0 until size) bitmap.setPixel(x,y,if(matrix[x,y]) Color.BLACK else Color.WHITE)
        return bitmap
    }
    private fun crc16(value:String):Int {
        var crc=0xFFFF
        value.toByteArray(Charsets.UTF_8).forEach { byte ->
            crc = crc xor ((byte.toInt() and 0xFF) shl 8)
            repeat(8) { crc = if((crc and 0x8000)!=0) ((crc shl 1) xor 0x1021) and 0xFFFF else (crc shl 1) and 0xFFFF }
        }
        return crc and 0xFFFF
    }
}
