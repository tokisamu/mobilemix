package com.paruyr.fluencytask.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresApi
import com.paruyr.fluencytask.ECC
import com.paruyr.fluencytask.ElGamel
import com.paruyr.fluencytask.FinitePrimeField
import com.paruyr.fluencytask.Point
import com.paruyr.fluencytask.PrimeCurve
import com.paruyr.fluencytask.SecretShare
import com.paruyr.fluencytask.domain.model.FluencyMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.security.SecureRandom
import java.util.Calendar
import java.util.UUID
import kotlin.math.max


interface BluetoothRepository {
    suspend fun sendMessage(message: FluencyMessage)
    fun observeMessages(): Flow<FluencyMessage>
    fun startDiscovery(): Flow<BluetoothDevice>?
    fun stopDiscovery()
    suspend fun connectToDevice(device: BluetoothDevice): Boolean
    suspend fun startListeningForConnections(): Flow<BluetoothDevice>?
    fun isConnected(): Boolean
    fun observeDisconnection(): Flow<Boolean>
    fun resetConnection() // Add reset function
    fun broadcastMessage(message: String) // Add reset function
    fun createGroup()
    fun joinGroup(groupID: Int)
    fun getParentDevice():BluetoothDevice?
}

// BluetoothRepositoryImpl: Implementation of the BluetoothRepository
class BluetoothRepositoryImpl(
    private val context: Context,
) : BluetoothRepository {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = bluetoothAdapter?.bluetoothLeAdvertiser
    private val _messages = MutableSharedFlow<FluencyMessage>()
    private val _discoveredDevices = MutableSharedFlow<BluetoothDevice>(replay = 1)
    private val _disconnectionHandler = MutableSharedFlow<Boolean>(replay = 1)
    private var bluetoothSocket: BluetoothSocket? = null
    private var serverSocket: BluetoothServerSocket? = null
    var current_index = 1;
    var current_stage = -1;
    var current_group_index =1;
    var current_message_cnt = 1;
    var messageString = "";
    var messageString2 = "";
    private var isConnected = false
    private var myGroupPartPrivateKey: BigInteger? = null
    private var myPartPublicKet: Point? = null
    private var testPrivateKey = BigInteger("0")
    private var groupPrivateKey: ArrayList<BigInteger>  = ArrayList<BigInteger>()
    private var myShares:Array<SecretShare>? = null
    private var mySecret: BigInteger? = null
    private var myGroupPublicKey: Point? = null
    private var groupPublicKey: ArrayList<Point>? = null
    private var collectedPublicKey: ArrayList<Point>? = ArrayList<Point>()
    private var collectedShares: ArrayList<Array<BigInteger>> = ArrayList<Array<BigInteger>>()
    private var groupID = arrayOf(0, 0, 0, 0, 0)
    private var setUpStage = 0;
    private var myGroupNum = 1;
    private var roundCnt = 0;
    private var numMember = arrayOf(0, 0, 0, 0, 0)
    private var groupOrder = arrayOf(0, 1, 2, 3, 4)
    private var storedDevices: ArrayList<BluetoothDevice>? = null
    private var myGroupID = 1
    private var parentDevice: BluetoothDevice? = null
    override fun getParentDevice(): BluetoothDevice? {
        return parentDevice;
    }
    private var myGroupNumMember = 1;
    private var myGroupIndex = 1;
    private var myGroupPrivateKey: ArrayList<BigInteger>  = ArrayList<BigInteger>()

    val ElGamel =  ElGamel();
    //Log.d("shenyu", ElGamel.generator.toString());
    //val hahaPriKey = ElGamel.random;
    //val hahaPubKey = ElGamel.getPuk(hahaPriKey);
    val p192 = BigInteger("6277101735386680763835789423207666416083908700390324961279")
    val z192 = FinitePrimeField(p192)
    val b192 = BigInteger("64210519e59c80e70fa7e9ab72243049feb8deecc146b9b1", 16)
    val E192 = PrimeCurve(z192, BigInteger.valueOf(-3), b192, 256)
    val B192: Point = E192.getPoint(BigInteger("188DA80EB03090F67CBF20EB43A18800F4FF0AFD82FF1012", 16))
    val n192 = BigInteger("6277101735386680763835789423176059013767194773182842284081")
    val k192 = ECC.ECDHPhase1(E192, B192, n192)
    val E = E192;
    val prime = n192;
    val B = B192;
    val h = E.getH()?.toLong()?.let { BigInteger.valueOf(it) }
    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    val maxM: BigInteger = E.getP().getP().divide(h).subtract(BigInteger.ONE)
    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    Log.d(
                        "BluetoothRepository",
                        "Discovered device: ${device.name ?: "Unknown"} - ${device.address}"
                    )
                    _discoveredDevices.tryEmit(it)
                }
            }
        }
    }

    override suspend fun sendMessage(message: FluencyMessage) {
        bluetoothSocket?.let { socket ->
            withContext(Dispatchers.IO) {
                try {
                    if (myShares == null) {
                        val CERTAINTY = 256
                        val random = SecureRandom()
                        var newKey = ECC.ECDHPhase1(E192, B192, n192);
                        collectedPublicKey?.add(newKey.publicKey)
                        myPartPublicKet = newKey.publicKey
                        var newSecret = newKey.privateKey
                        //broadcast generated publicKey
                        //newKey.publicKey
                        val prime = n192;
                        var newShares: Array<SecretShare> =
                            ECC.split(newSecret, 2, 3, prime, random)
                        myShares = newShares
                        mySecret = newKey.privateKey
                        newShares[myGroupIndex] //this is for this user
                        var sendToArray =
                            Array<BigInteger>(0) { BigInteger(myGroupIndex.toString()) }
                        for (i in 1..3)
                            sendToArray += newShares[i - 1].share
                        if (collectedShares.size<10)
                        {
                            for (i in 0..9)
                                collectedShares.add(arrayOf(BigInteger((i+1).toString())))
                        }
                        sendToArray += BigInteger(myGroupIndex.toString())
                        collectedShares.set(myGroupIndex,sendToArray)
                        Log.d("shenyuexception",collectedShares[0].contentToString())
                        Log.d("shenyuexception",collectedShares[1].contentToString())
                        val outputStream: OutputStream = socket.outputStream
                        Log.d("shenyumessage", newShares.toString())
                        //should encrypt for each user but simplified here
                        outputStream.write(("1"+sendToArray.contentToString()).toByteArray())
                        Log.d("BluetoothRepository", "Sent message: ${message.content}")
                        setUpStage = 1;
                    } else if (setUpStage ==1){
                        val outputStream: OutputStream = socket.outputStream
                        outputStream.write(("2"+myGroupIndex.toString()+myPartPublicKet.toString()).toByteArray())
                        Log.d("BluetoothRepository", "Sent message: ${message.content}")
                        setUpStage = 2;
                    }
                    else if (setUpStage ==2){
                        val outputStream: OutputStream = socket.outputStream
                        outputStream.write(("3"+myGroupIndex.toString()).toByteArray())
                        Log.d("BluetoothRepository", "Sent message: ${message.content}")
                        setUpStage = 4;
                    }
                    else if (setUpStage == 3) //generate new message
                    {
                        val m = ECC.randomBigInteger().mod(maxM)
                        Log.d("shenyumessage",m.toString())
                        Log.d("shenyupp1", m.toString());
                        var encoded: Array<Point> = ECC.ElGamalEnc(E, m, B, myGroupPublicKey)
                        val outputStream: OutputStream = socket.outputStream
                        //encoded = ECC.ElPartDec(E, B, encoded, myGroupPrivateKey?.get(myGroupID))
                        outputStream.write(("4"+encoded.contentToString()).toByteArray())
                        setUpStage = 4;
                    }
                    else if (setUpStage == 4) //generate 100 message to shuffle
                    {
                        var messageToSend = "";
                        val outputStream: OutputStream = socket.outputStream
                        var messages: ArrayList<BigInteger> = ArrayList<BigInteger>();
                        for (i in 0..99) {
                            val m = ECC.randomBigInteger().mod(maxM)
                            messages.add(m)
                            var encoded: Array<Point> = ECC.ElGamalEnc(E, m, B, myGroupPublicKey)
                            //encoded = ECC.ElPartDec(E, B, encoded, myGroupPrivateKey?.get(myGroupID))
                            var indexSend = ""
                            if(i<10)
                                indexSend+=" "
                            indexSend+=i.toString()
                            messageToSend = messageToSend +encoded.contentToString()
                            if(i==0)
                                outputStream.write(("4" + "0"+myGroupIndex.toString()+indexSend+encoded.contentToString()).toByteArray())
                            else
                                outputStream.write((encoded.contentToString()).toByteArray())
                            //outputStream.flush()
                        }
                        messageToSend = ""
                        outputStream.write("!".toByteArray())
                        Log.d("shuffleMessage",messageToSend)
                        for (i in 0..99) {
                            val m = messages[i]
                            var encoded: Array<Point> = ECC.ElGamalInverseEnc(E, m, B, myGroupPublicKey,maxM)
                            //encoded = ECC.ElPartDec(E, B, encoded, myGroupPrivateKey?.get(myGroupID))
                            var indexSend = ""
                            if(i<10)
                                indexSend+=" "
                            indexSend+=i.toString()
                            messageToSend = messageToSend +encoded.contentToString()
                            if(i==0)
                                outputStream.write(("4" + "0"+myGroupIndex.toString()+indexSend+encoded.contentToString()).toByteArray())
                            else
                                outputStream.write((encoded.contentToString()).toByteArray())
                            //outputStream.flush()
                        }
                        outputStream.write("!".toByteArray())
                        Log.d("shuffleMessage",messageToSend)
                        setUpStage = 5
                        for(i in 0..99)
                            _messages.emit(FluencyMessage(messages[i].toString()+" "+E.h.toString(),false))
                    }
                    else if (setUpStage == 5) //send 100 messages to decrypt
                    {
                        var messageToSend = "";
                        val outputStream: OutputStream = socket.outputStream
                        for (i in 0..99) {
                            val m = ECC.randomBigInteger().mod(maxM)
                            var encoded: Array<Point> = ECC.ElGamalEnc(E, m, B, myGroupPublicKey)
                            //encoded = ECC.ElPartDec(E, B, encoded, myGroupPrivateKey?.get(myGroupID))
                            var indexSend = ""
                            if(i<10)
                                indexSend+=" "
                            indexSend+=i.toString()
                            messageToSend = messageToSend +encoded.contentToString()
                            if(i==0)
                                outputStream.write(("5" + "0"+myGroupIndex.toString()+indexSend+encoded.contentToString()).toByteArray())
                            else
                                outputStream.write((encoded.contentToString()).toByteArray())
                            //outputStream.flush()
                        }
                        outputStream.write("!".toByteArray())
                        Log.d("shuffleMessage",messageToSend)
                        setUpStage = 5
                    }
                    else if (setUpStage == 6) //re-encrypt message
                    {

                    }
                    else
                    {
                        val outputStream: OutputStream = socket.outputStream
                        outputStream.write(("9"+message.content).toByteArray())
                        Log.d("BluetoothRepository", "Sent message: ${message.content}")
                    }
                } catch (e: Exception) {
                    Log.e("BluetoothRepository", "Failed to send message: ${e.message}")
                }
            }
        }
    }





    @RequiresApi(Build.VERSION_CODES.O)
    override fun createGroup()
    {
        if(myGroupID==0)
            myGroupID = 1;
        myGroupIndex = 0;
        myGroupNumMember = 1;
        numMember[myGroupID] = 1;
        broadcastMessage(myGroupID.toString() +" "+myGroupIndex.toString()+" "+numMember[myGroupID].toString())
    }

    //after there are enough users
    fun initializationOfGroupStage1()
    {
        val CERTAINTY = 256
        val random = SecureRandom()
        var newKey = ECC.ECDHPhase1(E192, B192, n192);
        var newSecret = newKey.privateKey
        //broadcast generated publicKey
        newKey.publicKey
        val prime =n192;
        var newShares: Array<SecretShare> = ECC.split(newSecret, 3, 3, prime, random)
        newShares+=SecretShare(myGroupIndex, BigInteger(myGroupIndex.toString()))
        newShares[myGroupIndex] //this is for this user
        for (i in 0..numMember[myGroupID])
        {
            if(i==myGroupIndex)
                continue;

            //send newShares[i] to the ith user
        }
    }

    //after information from other users are collected
    fun initializationOfGroupStage2()
    {
        val CERTAINTY = 256
        // prime number must be longer then secret number
        val prime =n192;
        var temPublicKey = E.sum(collectedPublicKey?.get(0) , collectedPublicKey?.get(1));
        for (i in 2..numMember[myGroupID])
            temPublicKey = E.sum(temPublicKey,collectedPublicKey?.get(i))
        myGroupPublicKey = temPublicKey;
        /*
        shares1[0].share = shares1[0].share.add(shares2[0].share).add(shares3[0].share)
        shares1[1].share = shares1[1].share.add(shares2[1].share).add(shares3[1].share)
        shares1[2].share = shares1[2].share.add(shares2[2].share).add(shares3[2].share)
        var sumPublicKey = E.sum(E.sum(key.publicKey,key2.publicKey),key3.publicKey)
        Log.d("shenyup1", sumPublicKey.toString());
        Log.d("shenyup1", E.mul(B,secret1.add(secret2).add(secret3).mod(prime)).toString());
        var part1 = ECC.getPartpri(shares1[0], prime,0,3)
        var part2 = ECC.getPartpri(shares1[1], prime,1,3)
        var part3 = ECC.getPartpri(shares1[2], prime,2,3)
        val m = ECC.randomBigInteger().mod(maxM)
        var encoded: Array<Point> = ECC.ElGamalEnc(E, m, B, sumPublicKey)
        var plaintext = ECC.ElGamalDec(E, B, encoded, part1.add(part2).add(part3).mod(prime))
        Log.d("shenyup1", plaintext.toString());
        Log.d("shenyup1", m.toString());
        //Log.d("shenyupha", ECC.ElGamalTwoDec(E, B, encoded, part1,part2).toString());
        encoded = ECC.ElPartDec(E, B, encoded, part1)
        encoded = ECC.ElPartDec(E, B, encoded, part2)
        plaintext = ECC.ElGamalDec(E, B, encoded, part3)
        Log.d("shenyup", plaintext.toString());
        Log.d("shenyup", m.toString());
        */
    }
    // Observe received messages
    override fun observeMessages(): Flow<FluencyMessage> = _messages.asSharedFlow()

    //crypt test
    //protocol process:
    //1: join a group or create a new group
    //2: get public key for the joined group and give keys for others
    //3: get the pub and private key of itself and trusted parties
    //4: generate a nym
    //5: get group signature of the nym
    //6: broadcast the nym
    //7: collect others' nym and calculate trusted parties' nym
    //8: collect incoming messages
    //9: shuffling messages n times if there are 1000 messages
    //10: decrypt the shuffled messages
    //11: for those 0 message, the key owner ask group re encrypt it with a specific key
    //12: the key owner assign another group to forward this message

    //Step 1,2 multi party computation to be done
    @RequiresApi(Build.VERSION_CODES.O)
    override fun joinGroup(targetGroupID: Int){
        myGroupID = targetGroupID;
        myGroupIndex = numMember[myGroupID];
        Log.d("shenyurr",myGroupID.toString()+" "+numMember[myGroupID].toString())
        numMember[myGroupID]++;
        myGroupNum = max(myGroupNum,numMember[myGroupID])
        broadcastMessage(myGroupID.toString() +" "+myGroupIndex.toString()+" "+numMember[myGroupID].toString())
    }

    //step 4,5,6 blind key to be done
    fun generateNym(numMember: Int):Int{
        return 0
    }

    //step7
    fun findTrustedByNym(numMember: Int):Int{
        return 0
    }

    //step9
    fun shuffleMessagePool():Int{
        return 0
    }

    //step10
    fun reEncryptMessagePool():Int{
        return 0
    }

    //step11
    fun encryptForTrustedAndBroadcast():Int{
        return 0
    }

    @SuppressLint("MissingPermission")
    private fun myScanCallback() = object : ScanCallback() {
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if (result != null) {
                val id = "00001101-0000-1000-8000-00805f9b34fb"
                val tempResult = result.scanRecord?.serviceData.toString();
                if(tempResult.length>37) {
                    val resultID = tempResult.substring(1, 37)
                    if (id == resultID)
                    {
                        var message: String;
                        message = "";
                        val chars = tempResult.substring(39, tempResult.length - 2).split(",")
                        //Log.d("shenyur", chars.toString())
                        for(i in 0..chars.size-1)
                        {
                            if(i==0)
                                message += chars[i].substring(0,chars[i].length).toInt().toChar() ;
                            else message += chars[i].substring(1,chars[i].length).toInt().toChar() ;
                        }
                        Log.d("shenyur", message)
                        val receivedGroupID = message.substring(0,1).toInt();
                        val receivedIndex = message.substring(2,3).toInt();
                        val receivedGroupNum= message.substring(4,5).toInt();
                        //Log.d("shenyur", receivedGroupNum.toString())
                        //if(receivedGroupID==myGroupID)
                        //just to test need modification later
                        storedDevices?.add(result.device)
                        parentDevice = result.device;
                        if (receivedGroupID ==myGroupID)
                        {
                            parentDevice = result.device;
                            Log.d("shenyudevice",result.device.name)
                        }
                        if (receivedGroupNum>numMember[receivedGroupID])
                        {
                            numMember[receivedGroupID] = receivedGroupNum;
                        }
                    }
                }
            }
            if(result!=null)
                Log.d("shenyu",result.toString())
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
        }
    }
    // Start Bluetooth discovery and also broadcast information

    @SuppressLint("MissingPermission")
    override fun startDiscovery(): Flow<BluetoothDevice>? {
        if (bluetoothAdapter?.isEnabled == true) {
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            context.registerReceiver(receiver, filter)
            bluetoothAdapter.startDiscovery()
            bluetoothAdapter.getBluetoothLeScanner().startScan(myScanCallback())
            Log.d("BluetoothRepository", "Bluetooth discovery started.")
            Log.d("shenyu","before advertise")
            //bluetoothLeAdvertiser?.startAdvertising(settings, data, sampleAdvertiseCallback())

            //crypt test
            //protocol process:
            //1: join a group or create a new group
            //2: get public key for the joined group and give keys for others
            //3: get the pub and private key of itself and trusted parties
            //4: generate a nym
            //6: get group signature of the nym
            //7: broadcast the nym
            //8: collect others' nym and calculate trusted parties' nym
            //9: collect incoming messages
            //10: shuffling messages n times if there are 1000 messages
            //11: decrypt the shuffled messages
            //12: for those 0 message, the key owner ask group re encrypt it with a specific key
            //13: the key owner assign another group to forward this message
            val ElGamel =  ElGamel();

            //Log.d("shenyu", ElGamel.generator.toString());
            //val hahaPriKey = ElGamel.random;
            //val hahaPubKey = ElGamel.getPuk(hahaPriKey);
            val k192 = ECC.ECDHPhase1(E192, B192, n192)
            val E = E192;
            val B = B192;
            val key = k192;
            //Log.d("shenyulog", E192.mul(B,p192.add(BigInteger.ONE)).toString())
            //Log.d("shenyulog", E192.mul(B,p192).toString())
            //Log.d("shenyulog", E192.mul(B,b192).toString())
            //Log.d("shenyulog", E192.mul(B,n192.add(BigInteger.ONE)).toString())
            //Log.d("shenyulog", B.toString())

            var key2 = ECC.ECDHPhase1(E192, B192, n192);
            var currentTime = Calendar.getInstance().time
            var messages = ArrayList<Array<Point>>();
            Log.d("shenyut", currentTime.toString());
            var plaintexts = ArrayList<BigInteger>();

            for (i in 0..0) {
                // generate a random integer and encrypt
                val m = ECC.randomBigInteger().mod(maxM)
                plaintexts.add(m);
                val encoded: Array<Point> = ECC.ElGamalEnc(E, m, B, key.getPublicKey())
                messages.add(encoded)
                // decrypt
                //val plaintext = ECC.ElGamalDec(E, B, encoded, key.getPrivateKey())

                // check
                //Log.d("shenyu1", m.toString());
                //Log.d("shenyu2", plaintext.toString());
            }
            currentTime = Calendar.getInstance().time
            //Log.d("shenyut", currentTime.toString());
            messages = ECC.ElGamalShuffle(E,B,messages,key.publicKey)
            currentTime = Calendar.getInstance().time
            //Log.d("shenyut", currentTime.toString());
            messages = ECC.ElGamalInitReencrypt(E,B,messages)
            ECC.ElGamalReencrypt(E,B,messages,key2.publicKey,key.privateKey)
            currentTime = Calendar.getInstance().time
            //Log.d("shenyut", currentTime.toString());
            for (i in 0..0) {
                // generate a random integer and encrypt
                val m = plaintexts[i];
                var encoded = messages[i];
                // decrypt
                val plaintext = ECC.ElGamalDec(E, B, encoded, key2.getPrivateKey())

                // check
                Log.d("shenyu1", m.toString());
                Log.d("shenyu2", plaintext.toString());
            }

            currentTime = Calendar.getInstance().time
            Log.d("shenyut", currentTime.toString());
            //val messages = new ArrayList<ElgamelMessage>();
            /*
            for(i in 1..1000)
            {

            }
            IntStream.range(0, 1000).parallel().forEach { i: Int ->
                val haha = ElGamel.encrypt(BigInteger("1"), hahaPubKey, ElGamel.random)
            }
            */
            /*
            for(i in 1..1000)
            {
                val haha = ElGamel.encrypt(BigInteger("1"), hahaPubKey, ElGamel.random)
                //Log.d("shenyu2", haha.getMessage().toString());
                //val plaintext = ElGamel.decrypt(haha, hahaPriKey);
                //Log.d("shenyu3", plaintext.toString());
            }*/
            /*
            val haha = ElGamel.encrypt(BigInteger("1"), hahaPubKey, ElGamel.random)
            Log.d("shenyu2", haha.getMessage().toString());
            val plaintext = ElGamel.decrypt(haha, hahaPriKey);
            Log.d("shenyu3", plaintext.toString());
            currentTime = Calendar.getInstance().time
            Log.d("shenyut", currentTime.toString());
            */


            val CERTAINTY = 256
            val random = SecureRandom()
            val secret1 = key.privateKey
            // prime number must be longer then secret number
            val prime =n192;
            //Log.d("shenyusorder", prime.toString());
            // 2 - at least 2 secret parts are needed to view secret
            // 5 - there are 5 persons that get secret parts
            var shares1: Array<SecretShare> = ECC.split(secret1, 2, 3, prime, random)
            key2 = ECC.ECDHPhase1(E192, B192, n192);
            var secret2 = key2.privateKey
            var shares2: Array<SecretShare> = ECC.split(secret2, 2, 3, prime, random)
            var key3 = ECC.ECDHPhase1(E192, B192, n192);
            var secret3 = key3.privateKey
            var shares3: Array<SecretShare> = ECC.split(secret3, 2, 3, prime, random)
            shares1[0].share = shares1[0].share.add(shares2[0].share).add(shares3[0].share)
            shares1[1].share = shares1[1].share.add(shares2[1].share).add(shares3[1].share)
            shares1[2].share = shares1[2].share.add(shares2[2].share).add(shares3[2].share)
            var tempri = ECC.combine(shares1,prime)
            Log.d("what is the result","tanoshimi");
            Log.d("shenyup", tempri.mod(prime).toString());
            Log.d("shenyup", secret1.add(secret2).add(secret3).mod(prime).toString());
            var sumPublicKey = E.sum(E.sum(key.publicKey,key2.publicKey),key3.publicKey)
            Log.d("shenyup1", sumPublicKey.toString());
            Log.d("shenyup1", E.mul(B,secret1.add(secret2).add(secret3).mod(prime)).toString());
            var part1 = ECC.getPartpri(shares1[0], prime,0,2)
            var part2 = ECC.getPartpri(shares1[1], prime,1,2)
            var part3 = ECC.getPartpri(shares1[2], prime,2,3)
            val m = ECC.randomBigInteger().mod(maxM)
            var encoded: Array<Point> = ECC.ElGamalEnc(E, m, B, sumPublicKey)
            var plaintext = ECC.ElGamalDec(E, B, encoded, part1.add(part2).add(part3).mod(prime))
            Log.d("shenyup1", plaintext.toString());
            Log.d("shenyup1", m.toString());
            //Log.d("shenyupha", ECC.ElGamalTwoDec(E, B, encoded, part1,part2).toString());
            encoded = ECC.ElPartDec(E, B, encoded, part1)
            //encoded = ECC.ElPartDec(E, B, encoded, part2)
            plaintext = ECC.ElGamalDec(E, B, encoded, part2)
            Log.d("shenyup", plaintext.toString());
            Log.d("shenyup", m.toString());
            // we can use any combination of 2 or more parts of secret
            // 0 & 1
            /*
            var part1 = ECC.getPartpri(shares[0], prime,0,4)
            var part2 = ECC.getPartpri(shares[1], prime,1,4)
            var part3 = ECC.getPartpri(shares[2], prime,2,4)
            var part4 = ECC.getPartpri(shares[3], prime,3,4)
            var num = part1.add(part2).add(part3).add(part4).divide(prime);
            Log.d("shenyusorder", key.privateKey.toString());
            Log.d("shenyusorder", part1.add(part2).add(part3).add(part4).toString());
            val m = ECC.randomBigInteger().mod(maxM)
            var encoded: Array<Point> = ECC.ElGamalEnc(E, m, B, key.getPublicKey())
            Log.d("shenyup", ECC.ElGamalDec(E, B, encoded, key.getPrivateKey()).toString());
            //Log.d("shenyupha", ECC.ElGamalTwoDec(E, B, encoded, part1,part2).toString());
            encoded = ECC.ElPartDec(E, B, encoded, part1)
            encoded = ECC.ElPartDec(E, B, encoded, part2)
            encoded = ECC.ElPartDec(E, B, encoded, part3)
            var plaintext = ECC.ElGamalDec(E, B, encoded, part4)
            Log.d("shenyup", plaintext.toString());
            Log.d("shenyup", m.toString());

             */

            return _discoveredDevices.asSharedFlow()
        } else {
            Log.e("BluetoothRepository", "Bluetooth is not enabled.")
            return null
        }
    };



    /*
    @SuppressLint("MissingPermission")
    override fun startDiscovery(): Flow<BluetoothDevice>? {
        if (bluetoothAdapter?.isEnabled == true) {
            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            context.registerReceiver(receiver, filter)
            bluetoothAdapter.startDiscovery()
            Log.d("BluetoothRepository", "Bluetooth discovery started.")
            return _discoveredDevices.asSharedFlow()
        } else {
            Log.e("BluetoothRepository", "Bluetooth is not enabled.")
            return null
        }
    }
    */

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingPermission")
    override fun broadcastMessage(message:String) {
        if (bluetoothAdapter?.isEnabled == true) {
            val settings: AdvertiseSettings = buildAdvertiseSettings()
            val data: AdvertiseData = buildAdvertiseData(message)
            val maxDataLength: Int = bluetoothAdapter.getLeMaximumAdvertisingDataLength()
            Log.d("shenyulen",maxDataLength.toString())
            bluetoothLeAdvertiser?.stopAdvertising(sampleAdvertiseCallback());
            bluetoothLeAdvertiser?.startAdvertising(settings, data, sampleAdvertiseCallback())
        }else {
            Log.e("BluetoothRepository", "Bluetooth is not enabled.")
        }
    }

    private fun buildAdvertiseData(broadcastMess:String): AdvertiseData {
        val rid  = MY_UUID
        Log.d("MYID", rid.toString())
        var mymess = "daodiyouga"
        Log.d("MYMessage", broadcastMess.toByteArray().toString())
        return AdvertiseData.Builder()
            .addServiceData(ParcelUuid(rid),broadcastMess.toByteArray()).setIncludeDeviceName(true).build();
    }
    private fun buildAdvertiseSettings() = AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
        .setTimeout(0).build()

    private fun sampleAdvertiseCallback() = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.d("shenyuf", "Advertising failed "+errorCode.toString())
            //broadcastFailureIntent(errorCode)
            //stopSelf()
        }

        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            Log.d("shenyus", "Advertising successfully started")
        }
    }

    // Stop Bluetooth discovery
    @SuppressLint("MissingPermission")
    override fun stopDiscovery() {
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter?.cancelDiscovery()
            Log.d("BluetoothRepository", "Bluetooth discovery stopped.")
        }
        context.unregisterReceiver(receiver)
    }

    // Connect to a discovered Bluetooth device
    @SuppressLint("MissingPermission")
    override suspend fun connectToDevice(device: BluetoothDevice): Boolean {
        return withContext(Dispatchers.IO) {
            try {

                bluetoothLeAdvertiser?.stopAdvertising(sampleAdvertiseCallback())
                bluetoothSocket?.close()
                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
                bluetoothAdapter?.cancelDiscovery()
                bluetoothSocket?.connect()
                isConnected = true
                Log.d("BluetoothRepository", "Connected to device: ${device.name}")
                startListeningForMessages(bluetoothSocket!!)
                true
            } catch (e: Exception) {
                Log.e("BluetoothRepository", "Failed to connect to device: ${e.message}")
                isConnected = false
                _disconnectionHandler.tryEmit(true) // Notify disconnection on failure
                false
            }
        }
    }

    // Start listening for connections as a server
    @SuppressLint("MissingPermission")
    override suspend fun startListeningForConnections(): Flow<BluetoothDevice>? {
        return withContext(Dispatchers.IO) {
            serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord("MyApp", MY_UUID)

            val clientDeviceFlow = MutableSharedFlow<BluetoothDevice>(replay = 1)
            GlobalScope.launch {
                try {
                    val socket = serverSocket?.accept()
                    bluetoothSocket = socket
                    socket?.remoteDevice?.let { device ->
                        Log.d("BluetoothRepository", "Client device connected: ${device.name}")
                        clientDeviceFlow.emit(device)
                        startListeningForMessages(socket)
                    }
                } catch (e: Exception) {
                    Log.e(
                        "BluetoothRepository",
                        "Error while listening for connections: ${e.message}"
                    )
                    _disconnectionHandler.tryEmit(true) // Notify disconnection if error happens
                }
            }
            clientDeviceFlow.asSharedFlow()
        }
    }

    // Observe disconnection
    override fun observeDisconnection(): Flow<Boolean> = _disconnectionHandler.asSharedFlow()

    // Check if the device is connected
    override fun isConnected(): Boolean = isConnected

    fun stringToBigIntegerArray(input: String): Array<BigInteger> {
        var temp = input.substring(2,input.length-1)
        // Split the string by a delimiter (e.g., space or comma) and convert to BigInteger
        return temp.split(",")  // Change the delimiter as needed (e.g., "," or " ")
            .map { BigInteger(it.trim()) }  // Convert each part to BigInteger
            .toTypedArray()  // Convert the list to an array
    }

    // Start listening for incoming messages
    private fun startListeningForMessages(socket: BluetoothSocket) {
        val inputStream: InputStream = socket.inputStream
        if (collectedShares.size<10)
        {
            for (i in 0..9) {
                collectedShares.add(arrayOf(BigInteger((i + 1).toString())))
                myGroupPrivateKey?.add(BigInteger((i).toString()))
                groupPrivateKey?.add(BigInteger((i).toString()))
            }
        }
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val buffer = ByteArray(1024)
                while (true) {
                    val bytes = inputStream.read(buffer)
                    if (bytes > 0) {
                        var incomingMessage = String(buffer, 0, bytes).trim()
                        if (incomingMessage.isNotBlank()) {
                            if (incomingMessage[0]=='1'&&current_stage==-1) {
                                var incomingArray = stringToBigIntegerArray(incomingMessage)
                                var arrayIndex = incomingArray[incomingArray.size - 1].toInt();
                                myGroupNum = max(myGroupNum,arrayIndex+1)
                                collectedShares.set(arrayIndex, incomingArray)
                                _messages.emit(FluencyMessage(collectedShares[arrayIndex].contentToString(), false))
                                Log.d(
                                    "BluetoothRepository",
                                    "Received message:" + collectedShares[arrayIndex].contentToString()
                                )
                            }
                            else if(incomingMessage[0]=='2'&&current_stage==-1)
                            {
                                var receivedIndex = incomingMessage[1].toString().toInt()
                                _messages.emit(FluencyMessage(receivedIndex.toString(), false))
                                incomingMessage = incomingMessage.substring(2,incomingMessage.length)
                                var parts = incomingMessage.split(',','=')
                                var receivedPublicKey = Point(BigInteger(parts[1]), BigInteger(parts[3]),0)
                                collectedPublicKey?.add(receivedPublicKey)
                                _messages.emit(FluencyMessage(parts[1].toString()+ parts[3].toString(), false))
                                _messages.emit(FluencyMessage("partPublicKey: "+incomingMessage, false))
                            }
                            else if(incomingMessage[0]=='3'&&current_stage==-1)
                            {
                                myGroupPublicKey = E.sum(collectedPublicKey?.get(0),collectedPublicKey?.get(1))
                                //_messages.emit(FluencyMessage("groupKey: "+myGroupPublicKey.toString(), false))
                                var keySize = collectedPublicKey?.size ?: 0
                                for (i in 2..keySize-1)
                                {
                                    myGroupPublicKey = E.sum(myGroupPublicKey,collectedPublicKey?.get(i))
                                }
                                var sumGroupPrivateKey = BigInteger("0");
                                //_messages.emit(FluencyMessage("share 0 0 : "+collectedShares[0][0].toString(), false))
                                //_messages.emit(FluencyMessage("share 0 1 : "+collectedShares[0][1].toString(), false))
                                //_messages.emit(FluencyMessage("share 1 0 : "+collectedShares[1][0].toString(), false))
                                //_messages.emit(FluencyMessage("share 0 1 : "+collectedShares[1][1].toString(), false))
                                for(i in 0..myGroupNum-1)
                                {
                                    var tempShre =  collectedShares[0][i]+collectedShares[1][i]
                                    for(j in 2..myGroupNum-1)
                                    {
                                        tempShre+=collectedShares[j][i]
                                    }
                                    tempShre = tempShre.mod(prime)
                                    groupPrivateKey[i] = ECC.getPartpri(SecretShare(i,tempShre), prime,i,2);
                                    _messages.emit(FluencyMessage("pri  : "+i.toString()+" "+ECC.getPartpri(SecretShare(i,tempShre), prime,i,2).toString(), false))
                                    sumGroupPrivateKey = sumGroupPrivateKey.add(ECC.getPartpri(SecretShare(i,tempShre), prime,i,2)).mod(prime)
                                }
                                _messages.emit(FluencyMessage("groupKey: "+myGroupPublicKey.toString(), false))
                                _messages.emit(FluencyMessage("prikey: "+sumGroupPrivateKey.toString(), false))
                                val m = ECC.randomBigInteger().mod(maxM)
                                var encoded: Array<Point> = ECC.ElGamalEnc(E, m, B, myGroupPublicKey)
                                var plaintext = ECC.ElGamalDec(E, B, encoded, groupPrivateKey[0].add(groupPrivateKey[1]).mod(prime))
                                var plaintext2 = ECC.ElGamalDec(E, B, encoded, sumGroupPrivateKey)
                                testPrivateKey = sumGroupPrivateKey
                                _messages.emit(FluencyMessage("plaintext2 : "+m.toString(), false) )
                                _messages.emit(FluencyMessage("plaintext1 : "+plaintext.toString(), false) )
                                _messages.emit(FluencyMessage("plaintext3 : "+plaintext2.toString(), false) )
                                //Log.d("shenyupha", ECC.ElGamalTwoDec(E, B, encoded, part1,part2).toString());
                                //encoded = ECC.ElPartDec(E, B, encoded, part1)
                                //encoded = ECC.ElPartDec(E, B, encoded, part2)
                                //plaintext = ECC.ElGamalDec(E, B, encoded, part2)
                                var newShare = collectedShares[0][myGroupIndex]+collectedShares[1][myGroupIndex]
                                for(i in 2..myGroupNum-1)
                                {
                                    newShare+=collectedShares[i][myGroupIndex]
                                }
                                var part1 = ECC.getPartpri(SecretShare(myGroupIndex,newShare), prime,myGroupIndex,2)
                                myGroupPrivateKey?.set(myGroupID,part1)
                                _messages.emit(FluencyMessage("my part key is : "+part1.toString(), false))
                            }
                            else if(incomingMessage[0]=='4'&&current_stage==-1)
                            {
                                current_stage = 4;
                                Log.d("received plaintext",incomingMessage)
                                if(roundCnt==0)
                                    messageString+=incomingMessage
                                else
                                    messageString2+=incomingMessage
                            }
                            else if(current_stage==4)
                            {
                                if(roundCnt==0)
                                    messageString+=incomingMessage
                                else
                                    messageString2+=incomingMessage
                                //_messages.emit(FluencyMessage("11111"+messageString, false))
                                //_messages.emit(FluencyMessage("22222"+messageString2, false))
                                if(incomingMessage.contains("!", ignoreCase = true))
                                {
                                    current_stage = -1
                                    if(roundCnt==1)
                                    {
                                        var parts = messageString.split(',','=')
                                        var parts2 = messageString2.split(',','=')
                                        var messages = ArrayList<Array<Point>>();
                                        Log.d("parts2",parts2[1])
                                        for(i in 0..99)
                                        {
                                            var receivedCipher1 = Point(BigInteger(parts[1+11*i]), BigInteger(parts[3+11*i]), 0)
                                            var receivedCipher2 = Point(BigInteger(parts[7+11*i]), BigInteger(parts[9+11*i]), 0)
                                            var receivedCipher3 = Point(BigInteger(parts2[1+11*i]), BigInteger(parts2[3+11*i]), 0)
                                            var receivedCipher4 = Point(BigInteger(parts2[7+11*i]), BigInteger(parts2[9+11*i]), 0)
                                            var pl1 = ECC.ElGamalDec(E,B, arrayOf(E.sum(receivedCipher1,receivedCipher3),E.sum(receivedCipher2,receivedCipher4)),testPrivateKey)
                                            var pl2 = ECC.ElGamalDec(E,B, arrayOf(receivedCipher3,receivedCipher4),testPrivateKey)
                                            //E.mul(receivedCipher1,receivedCipher3)
                                            //_messages.emit(FluencyMessage(pl1.multiply(pl2).mod(prime).toString(), false))
                                            _messages.emit(FluencyMessage(pl1.toString(), false))
                                            messages.add(arrayOf(receivedCipher1,receivedCipher2))
                                        }
                                        //messages = ECC.ElGamalShuffle(E,B,messages,myGroupPublicKey)
                                        //_messages.emit(FluencyMessage(messages[0].contentToString(), false))
                                        messageString = ""
                                        messageString2 = ""
                                    }
                                    roundCnt = 1-roundCnt;
                                }
                            }
                            else if(incomingMessage[0]=='5'&&current_stage==-1)
                            {
                                current_stage = 5;
                                Log.d("received plaintext",incomingMessage)
                            }
                            else if(current_stage==5)
                            {
                                messageString+=incomingMessage
                                if(incomingMessage.contains("!", ignoreCase = true))
                                {
                                    current_stage = -1
                                    var parts = messageString.split(',','=')
                                    var messages = ArrayList<Array<Point>>();
                                    for(i in 0..99)
                                    {
                                        var receivedCipher1 = Point(BigInteger(parts[1+11*i]), BigInteger(parts[3+11*i]), 0)
                                        var receivedCipher2 = Point(BigInteger(parts[7+11*i]), BigInteger(parts[9+11*i]), 0)
                                        messages.add(arrayOf(receivedCipher1,receivedCipher2))
                                        val partPlain = ECC.ElGamalDec(E,B, arrayOf(receivedCipher1,receivedCipher2),myGroupPrivateKey.get(myGroupID))
                                        _messages.emit(FluencyMessage(partPlain.toString(), false))
                                    }
                                    roundCnt = 1-roundCnt;
                                }
                            }
                            else if(incomingMessage[0]=='6'&&current_stage==-1)
                            {
                                incomingMessage = incomingMessage.substring(1,incomingMessage.length)
                                _messages.emit(FluencyMessage(incomingMessage, false))
                            }
                            else if(incomingMessage[0]=='7'&&current_stage==-1)
                            {
                                incomingMessage = incomingMessage.substring(1,incomingMessage.length)
                                _messages.emit(FluencyMessage(incomingMessage, false))
                            }
                            else if(incomingMessage[0]=='9'&&current_stage==-1)
                            {
                                incomingMessage = incomingMessage.substring(1,incomingMessage.length)
                                _messages.emit(FluencyMessage(incomingMessage, false))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d("shenyuexception",e.toString())
                Log.e("BluetoothRepository", "Connection lost: ${e.message}")
                isConnected = false
                _disconnectionHandler.tryEmit(true) // Notify disconnection
            }
        }
    }

    // Reset the Bluetooth connection state
    override fun resetConnection() {
        try {
            bluetoothSocket?.close()
            serverSocket?.close()
            bluetoothSocket = null
            serverSocket = null
            isConnected = false
            Log.d("BluetoothRepository", "Bluetooth connection reset.")
        } catch (e: Exception) {
            Log.e("BluetoothRepository", "Error resetting connection: ${e.message}")
        }
    }
}
