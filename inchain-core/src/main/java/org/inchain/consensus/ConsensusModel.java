package org.inchain.consensus;

import org.inchain.crypto.Sha256Hash;

/**
 * Created by ln on 2017-07-04.
 * 共识申请相关信息
 */
public class ConsensusModel {

    //共识申请交易ID
    private Sha256Hash txid;
    //申请人
    private byte[] applicant;
    //打包人
    private byte[] packager;

    public ConsensusModel(Sha256Hash txid, byte[] applicant, byte[] packager) {
        this.txid = txid;
        this.applicant = applicant;
        this.packager = packager;
    }

    public Sha256Hash getTxid() {
        return txid;
    }

    public void setTxid(Sha256Hash txid) {
        this.txid = txid;
    }

    public byte[] getApplicant() {
        return applicant;
    }

    public void setApplicant(byte[] applicant) {
        this.applicant = applicant;
    }

    public byte[] getPackager() {
        return packager;
    }

    public void setPackager(byte[] packager) {
        this.packager = packager;
    }
}
