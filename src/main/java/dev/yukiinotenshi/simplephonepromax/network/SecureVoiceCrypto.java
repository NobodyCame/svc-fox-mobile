package dev.yukiinotenshi.simplephonepromax.network;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** X25519 + HKDF-SHA256 + AES-256-GCM. Fresh ephemeral keys per call. */
public final class SecureVoiceCrypto implements AutoCloseable {
    private KeyPair pair;
    private byte[] sendKey,receiveKey,transcript;
    private long sent,received=-1;
    private boolean initiator;
    public SecureVoiceCrypto()throws Exception{pair=KeyPairGenerator.getInstance("X25519").generateKeyPair();}
    public String publicKey(){return Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());}
    private static byte[] bytes(String s){return s.getBytes(StandardCharsets.UTF_8);}
    private static byte[] mac(byte[] key,byte[] value)throws Exception{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));return mac.doFinal(value);}
    public void derive(String session,String source,String target,String sourceKey,String targetKey,boolean initiator)throws Exception {
        this.initiator=initiator;transcript=MessageDigest.getInstance("SHA-256").digest(bytes("yuki-secure-voice-v1\n"+session+"\n"+source+"\n"+target+"\n"+sourceKey+"\n"+targetKey));
        var remote=KeyFactory.getInstance("X25519").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(initiator?targetKey:sourceKey)));
        KeyAgreement agreement=KeyAgreement.getInstance("X25519");agreement.init(pair.getPrivate());agreement.doPhase(remote,true);byte[] shared=agreement.generateSecret();
        byte[] prk=mac(transcript,shared);Arrays.fill(shared,(byte)0);
        byte[] forward=mac(prk,bytes("source-to-target\u0001")),backward=mac(prk,bytes("target-to-source\u0001"));Arrays.fill(prk,(byte)0);
        sendKey=initiator?forward:backward;receiveKey=initiator?backward:forward;
    }
    public String comparison(){return HexFormat.ofDelimiter("-").formatHex(Arrays.copyOf(transcript,16));}
    private byte[] crypt(int mode,byte[] key,boolean direction,long sequence,byte[] data)throws Exception {
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");byte[] nonce=ByteBuffer.allocate(12).putInt(direction?1:2).putLong(sequence).array();
        cipher.init(mode,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,nonce));cipher.updateAAD(transcript);return cipher.doFinal(data);
    }
    public record Packet(long sequence,byte[] ciphertext){}
    public synchronized Packet encrypt(byte[] plain)throws Exception {if(sent>=1L<<52)throw new IllegalStateException("key exhausted");long seq=sent++;return new Packet(seq,crypt(Cipher.ENCRYPT_MODE,sendKey,initiator,seq,plain));}
    public synchronized byte[] decrypt(long seq,byte[] ciphertext)throws Exception {if(seq<0||seq<=received)throw new SecurityException("replayed audio");byte[] plain=crypt(Cipher.DECRYPT_MODE,receiveKey,!initiator,seq,ciphertext);received=seq;return plain;}
    public synchronized void close(){if(sendKey!=null)Arrays.fill(sendKey,(byte)0);if(receiveKey!=null)Arrays.fill(receiveKey,(byte)0);pair=null;sendKey=receiveKey=null;}
}

