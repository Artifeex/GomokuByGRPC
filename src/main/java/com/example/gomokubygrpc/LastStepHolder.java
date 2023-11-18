package com.example.gomokubygrpc;

import grpc.PlayerRole;

public class LastStepHolder {

    private PlayerRole lastStepPlayerRole;

    private int lastStepRowIndex;

    private int lastStepColumnIndex;


    public PlayerRole getLastStepPlayerRole() {
        return lastStepPlayerRole;
    }

    public int getLastStepRowIndex() {
        return lastStepRowIndex;
    }

    public int getLastStepColumnIndex() {
        return lastStepColumnIndex;
    }




    public void addNewStep(int rowIndex, int columnIndex, PlayerRole lastStepPlayerRole) {
            lastStepRowIndex = rowIndex;
            lastStepColumnIndex = columnIndex;
            this.lastStepPlayerRole = lastStepPlayerRole;
    }
}
