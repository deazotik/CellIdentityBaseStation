package com.example.topindianboykotlin1

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoWcdma
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.util.reflect.TypeInfo
import io.ktor.util.reflect.typeInfo
import kotlinx.coroutines.runBlocking

val httpClient = HttpClient(Android) {
    engine {
        // this: AndroidEngineConfig
        connectTimeout = 100_000
        socketTimeout = 100_000

    }
}

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var dataList: ArrayList<DataClass>
    lateinit var titleList: Array<String>

    //用于获取GPS
    private var locationManager: LocationManager? = null
    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            //若要实时获取GPS更新，可在此处理
            Log.i(TAG, "Location changed...")
            Log.i(TAG, "Latitude :        " + location.latitude)
            Log.i(TAG, "Longitude :       " + location.longitude)
        }

        override fun onStatusChanged(s: String, i: Int, bundle: Bundle) {}
        override fun onProviderEnabled(s: String) {}
        override fun onProviderDisabled(s: String) {}
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.resyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)

    }

    override fun onStart() {
        super.onStart()
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val provider = LocationManager.GPS_PROVIDER
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        locationManager!!.requestLocationUpdates(
            provider,
            1000,
            1f,
            locationListener
        ) //参数依次为provider（GPS，或者NETWORK_PROVIDER或PASSIVE_PROVIDER），执行更新的最小时间，执行更新的最小距离，更新后的listener
        //        locationManager.requestSingleUpdate(provider, locationListener, null);//或者仅仅进行单词更新
    }

    override fun onStop() {
        super.onStop()
        locationManager!!.removeUpdates(locationListener)
    }

    fun showGps(view: View?) {
        val tv = findViewById<TextView>(R.id.gps_value)
        if (locationManager != null) {
            val provider = LocationManager.GPS_PROVIDER
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                tv.text = "No permission"
                return
            }
            val lastKnownLocation = locationManager!!.getLastKnownLocation(provider)
            if (lastKnownLocation != null) {
                val lat = lastKnownLocation.latitude
                val lon = lastKnownLocation.longitude
                val alt = lastKnownLocation.altitude
                val acc = lastKnownLocation.accuracy
                val spd = lastKnownLocation.speed
                tv.text =
                    "Latitude: $lat, Longitude: $lon,\nAltitude: $alt, Accuracy: $acc, Speed: $spd"
            } else {
                tv.text = "GPS not obtained"
            }
        }
    }

    fun showCellinfo(view: View?) {
        runBlocking {

            Log.i("httpClient", httpClient.post("https://ktor.io/docs/welcome.html") {
                //bodyType = TypeInfo
                contentType(ContentType.Application.Json)
                //setBody()
            }.status.toString())
        }
        val tv = findViewById<TextView>(R.id.cell_value)
        var cellInfoList: List<CellInfo?>? = null
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            tv.text = "No permission"
            return
        }
        val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        cellInfoList = telephonyManager!!.allCellInfo
        if (cellInfoList == null) {
            tv.text = "getAllCellInfo() return null"
        } else if (cellInfoList.size == 0) {
            tv.text = "Base station list is empty"
        } else {
            val cellNumber = cellInfoList.size
            val main_BS: BaseStation? = bindData(cellInfoList[0])

            titleList = arrayOf()
            for (cellInfo in cellInfoList) {
                titleList += bindData(cellInfo).toString()
            }
            getData()
            
            tv.text =
                "Get " + cellNumber + " base stations, \nMain base station information:\n" + main_BS.toString()
            for (cellInfo in cellInfoList) {
                val bs: BaseStation? = bindData(cellInfo)
                Log.i(TAG, bs.toString())
            }
        }
    }
    private fun getData(){
        dataList = arrayListOf<DataClass>()
        for (i in titleList.indices){
            val dataClass = DataClass(titleList[i])
            dataList.add(dataClass)
        }
        recyclerView.adapter = AdapterClass(dataList)
    }

    private fun bindData(cellInfo: CellInfo?): BaseStation? {
        var baseStation: BaseStation? = null
        val tm = this.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        //基站有不同信号类型：2G，3G，4G
        if (cellInfo is CellInfoWcdma) {
            //联通3G
            val cellInfoWcdma = cellInfo
            val cellIdentityWcdma = cellInfoWcdma.cellIdentity
            baseStation = BaseStation()
            baseStation.setMccMnc(tm.simOperator)
            //baseStation.setOper_name(tm.simOperatorName)
            baseStation.setType("WCDMA")
            baseStation.setCid(cellIdentityWcdma.cid)
            baseStation.setLac(cellIdentityWcdma.lac)
            baseStation.setBsic_psc_pci(cellIdentityWcdma.psc)
            if (cellInfoWcdma.cellSignalStrength != null) {
                baseStation.setAsuLevel(cellInfoWcdma.cellSignalStrength.asuLevel) //Get the signal level as an asu value between 0..31, 99 is unknown Asu is calculated based on 3GPP RSRP.
                baseStation.setSignalLevel(cellInfoWcdma.cellSignalStrength.level) //Get signal level as an int from 0..4
                baseStation.setDbm(cellInfoWcdma.cellSignalStrength.dbm) //Get the signal strength as dBm
            }
        } else if (cellInfo is CellInfoLte) {
            //4G
            val cellInfoLte = cellInfo
            val cellIdentityLte = cellInfoLte.cellIdentity
            baseStation = BaseStation()
            baseStation.setMccMnc(tm.simOperator)
            //baseStation.setOper_name(tm.simOperatorName)
            baseStation.setType("LTE")
            baseStation.setCid(cellIdentityLte.ci)
            baseStation.setLac(cellIdentityLte.tac)
            baseStation.setBsic_psc_pci(cellIdentityLte.pci)
            if (cellInfoLte.cellSignalStrength != null) {
                baseStation.setAsuLevel(cellInfoLte.cellSignalStrength.asuLevel)
                baseStation.setSignalLevel(cellInfoLte.cellSignalStrength.level)
                baseStation.setDbm(cellInfoLte.cellSignalStrength.dbm)
            }
        } else if (cellInfo is CellInfoGsm) {
            //2G
            val cellInfoGsm = cellInfo
            val cellIdentityGsm = cellInfoGsm.cellIdentity
            baseStation = BaseStation()
            baseStation.setMccMnc(tm.simOperator)
            //baseStation.setOper_name(tm.simOperatorName)
            baseStation.setType("GSM")
            baseStation.setCid(cellIdentityGsm.cid)
            baseStation.setLac(cellIdentityGsm.lac)
            if (cellInfoGsm.cellSignalStrength != null) {
                baseStation.setAsuLevel(cellInfoGsm.cellSignalStrength.asuLevel)
                baseStation.setSignalLevel(cellInfoGsm.cellSignalStrength.level)
                baseStation.setDbm(cellInfoGsm.cellSignalStrength.dbm)
            }
        } else {
            //电信2/3G
            Log.e(TAG, "CDMA CellInfo................................................")
        }
        return baseStation
    }

}




