package com.jolttime.game

import com.jolttime.game.data.local.GameStateCodec
import com.jolttime.game.domain.model.GameState
import org.junit.Assert.*
import org.junit.Test

class GameStateCodecTest {
    @Test fun rpgSaveRoundTripPreservesTeamAndCampaign(){val expected=GameState(storyIntroSeen=true,unlockedMissionIndex=3,completedMissionIds=setOf("portal","nile"),selectedTeam=listOf("mira","kael"),languageTag="uk");val restored=GameStateCodec.decodeOrDefault(GameStateCodec.encode(expected));assertEquals(expected.selectedTeam,restored.selectedTeam);assertEquals(expected.completedMissionIds,restored.completedMissionIds);assertEquals("uk",restored.languageTag)}
    @Test fun corruptJsonFallsBackSafely(){val restored=GameStateCodec.decodeOrDefault("{broken",42_000);assertEquals(0,restored.unlockedMissionIndex);assertEquals(42_000,restored.lastSeenAt);assertEquals(4,restored.heroes.size)}
    @Test fun oldClickerSaveMigratesWithoutCrash(){val old="""{"level":12,"xp":345,"timeShards":9876,"coins":543,"totalTaps":1234,"upgrades":[],"artifacts":[{"id":"egypt_0","name":"Scarab"}],"dailyDay":4,"lastSeenAt":1000}""";val restored=GameStateCodec.decodeOrDefault(old,2000);assertEquals(2,restored.saveVersion);assertEquals(12,restored.level);assertEquals(9876L,restored.timeShards);assertEquals(4,restored.heroes.size);assertEquals("egypt",restored.currentEpochId)}
    @Test fun invalidPersistedStateIsSanitized(){val invalid=GameState(unlockedMissionIndex=999,upgradeMaterials=-4,selectedTeam=listOf("missing"),level=-2,lastSeenAt=Long.MAX_VALUE);val restored=GameStateCodec.decodeOrDefault(GameStateCodec.encode(invalid),50000);assertEquals(0,restored.upgradeMaterials);assertEquals(4,restored.selectedTeam.size);assertTrue(restored.lastSeenAt<=50000)}
}
