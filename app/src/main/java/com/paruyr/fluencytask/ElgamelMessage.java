package com.paruyr.fluencytask;
import java.math.BigInteger;

public class ElgamelMessage {
    private BigInteger message;
    private BigInteger randomness;
    private BigInteger oldRandomness;
    ElgamelMessage(BigInteger m, BigInteger r)
    {
        this.message = m;
        this.randomness = r;
        this.oldRandomness = r;
    }
    public BigInteger getMessage() {
        return message;
    }

    public void setMessage(BigInteger message) {
        this.message = message;
    }

    public BigInteger getRandomness() {
        return randomness;
    }

    public void setRandomness(BigInteger randomness) {
        this.randomness = randomness;
    }

    public BigInteger getOldRandomness() {
        return oldRandomness;
    }

    public void setOldRandomness(BigInteger oldRandomness) {
        this.oldRandomness = oldRandomness;
    }
}
