package com.paruyr.fluencytask;
import java.math.BigInteger;
import java.util.ArrayList;

public class UserKeys {
    private BigInteger priKey;
    private BigInteger pubKey;
    private BigInteger nym;
    private BigInteger generator;
    private BigInteger userGroupKey;
    private ArrayList<BigInteger> groupKey;
    private ArrayList<BigInteger> trustList;
    private ArrayList<BigInteger> trustKey;

    public UserKeys(BigInteger priKey, BigInteger pubKey, BigInteger nym, BigInteger userGroupKey, ArrayList<BigInteger> groupKey, ArrayList<BigInteger> trustList, ArrayList<BigInteger> trustKey,BigInteger generator) {
        this.priKey = priKey;
        this.pubKey = pubKey;
        this.nym = nym;
        this.userGroupKey = userGroupKey;
        this.groupKey = groupKey;
        this.trustList = trustList;
        this.trustKey = trustKey;
        this.generator = generator;
    }

    public BigInteger getPriKey() {
        return priKey;
    }

    public void setPriKey(BigInteger priKey) {
        this.priKey = priKey;
    }

    public BigInteger getPubKey() {
        return pubKey;
    }

    public void setPubKey(BigInteger pubKey) {
        this.pubKey = pubKey;
    }

    public BigInteger getNym() {
        return nym;
    }

    public void setNym(BigInteger nym) {
        this.nym = nym;
    }

    public BigInteger getUserGroupKey() {
        return userGroupKey;
    }

    public void setUserGroupKey(BigInteger userGroupKey) {
        this.userGroupKey = userGroupKey;
    }

    public ArrayList<BigInteger> getGroupKey() {
        return groupKey;
    }

    public void setGroupKey(ArrayList<BigInteger> groupKey) {
        this.groupKey = groupKey;
    }

    public ArrayList<BigInteger> getTrustList() {
        return trustList;
    }

    public void setTrustList(ArrayList<BigInteger> trustList) {
        this.trustList = trustList;
    }

    public ArrayList<BigInteger> getTrustKey() {
        return trustKey;
    }

    public void setTrustKey(ArrayList<BigInteger> trustKey) {
        this.trustKey = trustKey;
    }
}
