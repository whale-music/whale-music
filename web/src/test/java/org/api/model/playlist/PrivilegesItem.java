package org.api.model.playlist;

import lombok.Data;

import java.util.List;

@Data
public class PrivilegesItem {
    private int flag;
    private String dlLevel;
    private int subp;
    private int fl;
    private int fee;
    private int dl;
    private String plLevel;
    private String maxBrLevel;
    private int maxbr;
    private Long id;
    private int sp;
    private int payed;
    private Object rscl;
    private int st;
    private List<ChargeInfoListItem> chargeInfoList;
    private FreeTrialPrivilege freeTrialPrivilege;
    private int downloadMaxbr;
    private String downloadMaxBrLevel;
    private int cp;
    private boolean preSell;
    private String playMaxBrLevel;
    private boolean cs;
    private boolean toast;
    private int playMaxbr;
    private String flLevel;
    private int pl;
}