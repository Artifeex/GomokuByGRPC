package com.example.gomokubygrpc;

import com.google.protobuf.Empty;
import grpc.*;
import grpc.EnemyTurnMessage;
import grpc.GameServiceGrpc;
import grpc.GameStatus;
import grpc.GameStatusMessage;
import grpc.PlayerRole;
import grpc.StartGameMessage;
import grpc.TurnMessageRequest;
import grpc.TurnMessageResponse;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

public class GameService extends GameServiceGrpc.GameServiceImplBase {

    private static final int PORT = 8080;

    private Game game = new Game();

    private PlayerRole activePlayer = PlayerRole.Cross;

    private int connectedPlayers = 0;

    private LastStepHolder holder = new LastStepHolder();

    @Override
    public void startGame(Empty request, StreamObserver<StartGameMessage> responseObserver) {
        connectedPlayers++;
        if (connectedPlayers == 1) {
            StartGameMessage response = StartGameMessage.newBuilder()
                    .setGameStatus(GameStatus.WaitingPlayers)
                    .setPlayerRole(PlayerRole.Cross)
                    .setActivePlayerRole(PlayerRole.Cross)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } else if (connectedPlayers == 2) {
            StartGameMessage response = StartGameMessage.newBuilder()
                    .setGameStatus(GameStatus.WaitingPlayers)
                    .setPlayerRole(PlayerRole.Zero)
                    .setActivePlayerRole(PlayerRole.Cross)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } else {

        }
    }

    @Override
    public void makeTurn(TurnMessageRequest request, StreamObserver<TurnMessageResponse> responseObserver) {
        TurnMessageResponse response;
        if (!game.turn(request.getRowIndex(), request.getColumnIndex(), request.getMadeMove())) {
            response = TurnMessageResponse.newBuilder()
                    .setIsCorrectTurn(false)
                    .build();
        } else {
            //добавить ход в LastStepHolder
            activePlayer = getEnemyRole(request.getMadeMove());
            holder.addNewStep(request.getRowIndex(), request.getColumnIndex(), request.getMadeMove());
            response = TurnMessageResponse.newBuilder()
                    .setIsCorrectTurn(true)
                    .setRowIndex(request.getRowIndex())
                    .setColumnIndex(request.getColumnIndex())
                    .setMadeMove(request.getMadeMove())
                    .setGameStatus(game.gameStatus)
                    .setActiveRole(activePlayer)
                    .build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getGameStatus(Empty request, StreamObserver<GameStatusMessage> responseObserver) {
        GameStatusMessage response;
        if (connectedPlayers >= 2) {
            response = GameStatusMessage.newBuilder()
                    .setGameStatus(GameStatus.GameStarted)
                    .build();
        } else {
            response = GameStatusMessage.newBuilder().setGameStatus(GameStatus.WaitingPlayers).build();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private PlayerRole getEnemyRole(PlayerRole playerRole) {
        return playerRole == PlayerRole.Cross ? PlayerRole.Zero : PlayerRole.Cross;
    }

    @Override
    public void getEnemyTurn(grpc.EnemyRoleRequest request, StreamObserver<EnemyTurnMessage> responseObserver) {
        EnemyTurnMessage response;
        if (holder.getLastStepPlayerRole() == null || holder.getLastStepPlayerRole() == request.getMyRole()) {
            response = EnemyTurnMessage.newBuilder().setEnemyTurned(false).build();
        } else {
            response = createEnemyTurnMessage();
        }
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    public static void main(String[] args) throws Exception {
        Server server = ServerBuilder.forPort(PORT).addService(new GameService()).build();
        server.start();
        System.out.println("Server started");
        server.awaitTermination();
    }

    private EnemyTurnMessage createEnemyTurnMessage() {
        return EnemyTurnMessage.newBuilder()
                .setEnemyTurned(true)
                .setRowIndex(holder.getLastStepRowIndex())
                .setColumnIndex(holder.getLastStepColumnIndex())
                .setMadeMove(holder.getLastStepPlayerRole())
                .setActiveRole(activePlayer)
                .setGameStatus(game.gameStatus)
                .build();
    }
}
