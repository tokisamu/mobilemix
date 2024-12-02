package com.paruyr.fluencytask;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Random;

public class ECC {

    // Codifica di Koblitz del messaggio m sulla curva prima P
    public static Point KoblitzEnc(PrimeCurve P, BigInteger m) {
        Integer h = P.getH();
        BigInteger x = m.multiply(BigInteger.valueOf(h)); // Calcolo x = m*h
        for (int i = 0; i < h; i++) {
            // Calcolo il punto ad ascissa x, se è valido lo restituisco
            Point p = P.getPoint(x);
            if (p.isValid()) return p;
            // Altrimenti incremento x e ripeto
            x = x.add(BigInteger.ONE);
        }
        // Se non ho trovato nessun punto, ne restituisco uno invalido
        return Point.INVALID;
    }

    public static Point KoblitzInverseEnc(PrimeCurve P, BigInteger m,BigInteger maxM) {
        Integer h = P.getH();
        BigInteger x = m.multiply(BigInteger.valueOf(h)); // Calcolo x = m*h
        for (int i = 0; i < h; i++) {
            // Calcolo il punto ad ascissa x, se è valido lo restituisco
            Point p = P.getPoint(x);
            if (p.isValid()) return P.pointInverse(p);
            // Altrimenti incremento x e ripeto
            x = x.add(BigInteger.ONE);
        }
        // Se non ho trovato nessun punto, ne restituisco uno invalido
        return Point.INVALID;
    }

    // Decodifica di Koblitz del punto p sulla curva prima P
    public static BigInteger KoblitzDec(PrimeCurve P, Point p) {
        Integer h = P.getH();
        BigInteger x = p.getX().divide(BigInteger.valueOf(h));
        return x;
    }



    public static Point[] ElGamalEnc(PrimeCurve P, BigInteger m, Point B, Point Pd) {
        Point pm = ECC.KoblitzEnc(P, m);
        BigInteger r = ECC.randomBigInteger();
        Point V = P.mul(B, r);
        Point rPd = P.mul(Pd, r);
        Point W = P.sum(pm, rPd);
        return new Point[]{V, W};
    }

    public static Point[] ElGamalInverseEnc(PrimeCurve P, BigInteger m, Point B, Point Pd,BigInteger maxM) {
        Point pm = ECC.KoblitzInverseEnc(P, m,maxM);
        BigInteger r = ECC.randomBigInteger();
        Point V = P.mul(B, r);
        Point rPd = P.mul(Pd, r);
        Point W = P.sum(pm, rPd);
        return new Point[]{V, W};
    }


    // Decodifica del messaggio (V, W) sulla curva prima P
    public static BigInteger ElGamalDec(PrimeCurve P, Point B, Point[] VW, BigInteger nD) {
        Point V = VW[0];
        Point W = VW[1];
        Point nDV = P.mul(V, nD);
        Point Pm = P.sub(W, nDV);
        BigInteger m = ECC.KoblitzDec(P, Pm);
        return m;
    }


    public static Point[] ElPartDec(PrimeCurve P, Point B, Point[] VW, BigInteger nD) {
        Point V = VW[0];
        Point W = VW[1];
        Point nDV = P.mul(V, nD);
        Point Pm = P.sub(W, nDV);
        BigInteger m = ECC.KoblitzDec(P, Pm);
        return new Point[]{V, Pm};
    }
    //test pass
    public static ArrayList<Point[]> ElGamalShuffle(PrimeCurve P, Point B, ArrayList<Point[]> VWs, Point Pd)
    {
        for (Point[] VW:VWs)
        {
            Point V = VW[0];
            Point W = VW[1];
            BigInteger r = ECC.randomBigInteger();
            V = P.sum(P.mul(B, r),V);
            Point rPd = P.mul(Pd, r);
            W = P.sum(W, rPd);
            VW[0] = V;
            VW[1] = W;
        }
        return VWs;
    }

    //test pass
    public static ArrayList<Point[]> ElGamalInitReencrypt(PrimeCurve P, Point B, ArrayList<Point[]> VWs)
    {
        int i = 0;
        for (Point[] VW:VWs)
        {
            Point newarr[] = new Point[3];
            newarr[0] = P.sub(B,B);
            newarr[1] = VW[1];
            newarr[2] = VW[0];
            VWs.set(i, newarr);
            i++;
        }
        return VWs;
    }
    //to be done
    public static ArrayList<Point[]> ElGamalReencrypt(PrimeCurve P, Point B, ArrayList<Point[]> VWs, Point Pd,BigInteger nD)
    {
        for (Point[] VW:VWs)
        {
            Point V = VW[0];
            Point W = VW[1];
            Point oldV = VW[2];
            BigInteger r = ECC.randomBigInteger();
            V = P.sum(P.mul(B, r),V);
            Point rPd = P.mul(Pd, r);
            W = P.sum(W, rPd);

            Point nDV = P.mul(oldV, nD);
            Point Pm = P.sub(W, nDV);
            BigInteger m = ECC.KoblitzDec(P, Pm);
            VW[0] = V;
            VW[1] = Pm;
            VW[2] = oldV;
        }
        return VWs;
    }

    // Prima fase dell'algoritmo ECDH, genera chiave privata nX e e chiave pubblica Px
    public static ECDHKey ECDHPhase1(PrimeCurve P, Point B, BigInteger n) {
        BigInteger nX = ECC.randomBigInteger().abs().mod(n);
        Point pX = P.mul(B, nX);
        ECDHKey key = new ECDHKey(nX, pX);
        return key;
    }

    // Seconda fase dell'algoritmo, genera il punto S in comune ai due utenti
    public static Point ECDHPhase2(PrimeCurve P, ECDHKey priv, Point pub) {
        BigInteger nX = priv.getPrivateKey();
        Point S = P.mul(pub, nX);
        return S;
    }

    // Firma digitale del messaggio m
    public static BigInteger[] ECDSASign(PrimeCurve P, Point B, BigInteger n, BigInteger m, ECDHKey key) {
        Point Q;
        BigInteger k, r, s;
        do {
            do {
                k = ECC.randomBigInteger().mod(n.subtract(BigInteger.valueOf(1))).add(BigInteger.ONE);
                Q = P.mul(B, k);
                r = Q.getX().mod(n);
            }
            while (r.compareTo(BigInteger.ZERO) == 0);
            BigInteger priv = key.getPrivateKey();
            BigInteger kinv = k.modInverse(n);
            s = r.multiply(priv).add(m).multiply(kinv).mod(n);
        }
        while (s.compareTo(BigInteger.ZERO) == 0);
        return new BigInteger[]{r, s};
    }

    public static Boolean ECDSAVerify(PrimeCurve P, Point B, BigInteger n, BigInteger[] sign, BigInteger m, ECDHKey key) {
        BigInteger r = sign[0];
        BigInteger s = sign[1];
        if (r.compareTo(BigInteger.ONE) < 0) return false;
        if (s.compareTo(BigInteger.ONE) < 0) return false;
        if (r.compareTo(n) >= 0) return false;
        if (s.compareTo(n) >= 0) return false;
        BigInteger w = s.modInverse(n);
        BigInteger u1 = m.multiply(w).mod(n);
        BigInteger u2 = r.multiply(w).mod(n);
        Point Pd = key.getPublicKey();
        Point Q = P.sum(P.mul(B, u1), P.mul(Pd, u2));
        if (Q.isInfinity()) return false;
        BigInteger v = Q.getX().mod(n);
        if (v.compareTo(r) != 0) return false;
        return true;
    }

    public static BigInteger randomBigInteger() {
        BigInteger n = new BigInteger("12345678987654321");
        Random rnd = new Random();
        int bitLength = n.bitLength();
        BigInteger ret;
        do {
            ret = new BigInteger(bitLength, rnd);
        } while (ret.compareTo(n) > 0);
        return ret;
    }

    public static SecretShare[] split(BigInteger secret, int needed, int available, BigInteger prime, Random random)
    {
        BigInteger[] coeff = new BigInteger[needed];
        coeff[0] = secret;
        for (int i = 1; i < needed; i++)
        {
            BigInteger r;
            while (true)
            {
                r = new BigInteger(prime.bitLength(), random);
                if (r.compareTo(BigInteger.ZERO) > 0 && r.compareTo(prime) < 0)
                {
                    break;
                }
            }
            coeff[i] = r;
        }

        SecretShare[] shares = new SecretShare[available];
        for (int x = 1; x <= available; x++)
        {
            BigInteger accum = secret;

            for (int exp = 1; exp < needed; exp++)
            {
                accum = accum.add(coeff[exp].multiply(BigInteger.valueOf(x).pow(exp).mod(prime))).mod(prime);
            }
            shares[x - 1] = new SecretShare(x, accum);
            //System.out.println("Share " + shares[x - 1]);
        }
        return shares;
    }

    public static BigInteger combine(SecretShare[] shares,BigInteger prime)
    {
        BigInteger accum = BigInteger.ZERO;

        for(int formula = 0; formula < shares.length; formula++)
        {
            BigInteger numerator = BigInteger.ONE;
            BigInteger denominator = BigInteger.ONE;

            for(int count = 0; count < shares.length; count++)
            {
                if(formula == count)
                    continue; // If not the same value

                int startposition = shares[formula].getNumber();
                int nextposition = shares[count].getNumber();

                numerator = numerator.multiply(BigInteger.valueOf(nextposition).negate()).mod(prime); // (numerator * -nextposition) % prime;
                denominator = denominator.multiply(BigInteger.valueOf(startposition - nextposition)).mod(prime); // (denominator * (startposition - nextposition)) % prime;
            }
            BigInteger value = shares[formula].getShare();
            BigInteger tmp = value.multiply(numerator).multiply(denominator.modInverse(prime));
            accum = prime.add(accum).add(tmp).mod(prime); //  (prime + accum + (value * numerator * modInverse(denominator))) % prime;
        }

        //System.out.println("The secret is: " + accum + "\n");

        return accum;
    }
    public static BigInteger getPartpri(SecretShare share,BigInteger prime,int index,int len)
    {
        BigInteger accum = BigInteger.ZERO;
        int formula = index;
        BigInteger numerator = BigInteger.ONE;
        BigInteger denominator = BigInteger.ONE;
        for(int count = 0; count < len; count++)
        {
            if(formula == count)
                continue; // If not the same value

            int startposition = index+1;
            int nextposition = count+1;

            numerator = numerator.multiply(BigInteger.valueOf(nextposition).negate()).mod(prime); // (numerator * -nextposition) % prime;
            denominator = denominator.multiply(BigInteger.valueOf(startposition - nextposition)).mod(prime); // (denominator * (startposition - nextposition)) % prime;
        }
        BigInteger value = share.getShare();
        BigInteger tmp = value.multiply(numerator).multiply(denominator.modInverse(prime));
        accum = prime.add(accum).add(tmp).mod(prime); //  (prime + accum + (value * numerator * modInverse(denominator))) % prime;
        //System.out.println("The secret is: " + accum + "\n");
        return accum;
    }
}
