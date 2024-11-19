package com.paruyr.fluencytask;
import java.util.*;
import java.security.*;
import java.math.BigInteger;

public class ElGamel {
    //p is mod
    Random random = new Random();
    BigInteger prime = new BigInteger("129051193528069107579058122988241221047879564412564996111361041917660725037371413155831552578975676706756509228987936816390097075837632660864558551270037446273780165011555840291840178888226507680700110893816961916252067140819279987459032737930001390394798905263118272842345041842413296304683863686405560554789");
    BigInteger generator = new BigInteger("2");
    BigInteger g = generator;
    BigInteger p = prime;
    public BigInteger getGenerator() {
        return generator;
    }
    public BigInteger getPrime() {
        return prime;
    }
    public BigInteger getRandom() {
        return BigInteger.probablePrime(1024, random).mod(generator);
    }
    public BigInteger getPuk(BigInteger pri) {
        return g.modPow(pri,p);
    }
    public ElgamelMessage encrypt(BigInteger message,BigInteger pubKey,BigInteger randomNum) {
        BigInteger r = randomNum;
        BigInteger EC = message.multiply(pubKey.modPow(r, p)).mod(p);
        BigInteger brmodp = g.modPow(r, p);
        return new ElgamelMessage(EC,brmodp);
    }
    //r is randomness
    public BigInteger decrypt(ElgamelMessage message,BigInteger priKey) {
        return message.getRandomness().modPow(priKey,p).modInverse(p).multiply(message.getMessage()).mod(p);
    }
    public ArrayList<ElgamelMessage> shuffle(BigInteger pubKey,ArrayList<ElgamelMessage> messages) {
        for(ElgamelMessage message: messages)
        {
            Random rand = new Random();
            BigInteger r = new BigInteger(p.bitLength(), rand).mod(p);
            message.setMessage(message.getMessage().multiply(pubKey.multiply(r).mod(p)).mod(p));
            message.setRandomness(message.getRandomness().multiply(g.multiply(r).mod(p)).mod(p));
        }
        return messages;
    }
    public ArrayList<ElgamelMessage> reencrypt(BigInteger pubKey,ArrayList<ElgamelMessage> messages,ArrayList<BigInteger> originalG,BigInteger priKey) {
        for(ElgamelMessage message: messages)
        {
            Random rand = new Random();
            BigInteger r = new BigInteger(p.bitLength(), rand).mod(p);
            message.setMessage(message.getMessage().multiply(pubKey.multiply(r).mod(p)).mod(p));
            message.setRandomness(message.getRandomness().multiply(g.multiply(r).mod(p)).mod(p));
        }
        return messages;
    }
    public int sign(BigInteger message,BigInteger priKey) {
        return 0;
    }
    public int verify(BigInteger message,BigInteger pubKey) {
        return 0;
    }
}
