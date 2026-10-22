package com.optima.tuner.services;

import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import com.optima.tuner.Optima;

/**
 * Quick Settings tile for Optima.
 */
public class OptimaTileService extends TileService {
    @Override
    public void onTileAdded() {
        super.onTileAdded();
        updateTile();
    }

    @Override
    public void onTileRemoved() {
        super.onTileRemoved();
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        Tile tile = getQsTile();
        boolean active = tile.getState() == Tile.STATE_ACTIVE;
        tile.setState(active ? Tile.STATE_INACTIVE : Tile.STATE_ACTIVE);
        tile.updateTile();
        Optima.putBool("qs_tile_active", tile.getState() == Tile.STATE_ACTIVE);

        // Start/stop keep-alive accordingly
        Intent intent = new Intent(this, KeepAliveService.class);
        if (tile.getState() == Tile.STATE_ACTIVE) {
            startForegroundService(intent);
        } else {
            stopService(intent);
        }
    }

    private void updateTile() {
        Tile tile = getQsTile();
        boolean active = Optima.getBool("qs_tile_active", false);
        tile.setState(active ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }
}
