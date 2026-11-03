package dev.galar.rank;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Treinadores do ranking. Edite aqui os times e as classes. */
public final class Trainers {
    /** reqClass: 0 Normal, 1 Ultra, 2 Master. tier 1..5 para treinadores de rota (0 para lideres/campeao). */
    public record Trainer(String id, String name, int tier, int reqClass, boolean leader, boolean champion, int skill, List<String> team) {}

    public static final Map<String, Trainer> ALL = new LinkedHashMap<>();

    private static void add(Trainer t) { ALL.put(t.id(), t); }

    static {
        add(new Trainer("teste", "Treinador Teste", 1, 0, false, false, 1, List.of("wooloo level=5")));
        // --- treinadores de rota (classe Normal)
        add(new Trainer("cadete", "Cadete", 1, 0, false, false, 1, List.of("rookidee level=7", "wooloo level=8")));
        add(new Trainer("marcos", "Marcos", 2, 0, false, false, 1, List.of("corvisquire level=16", "dubwool level=17", "thievul level=17")));
        add(new Trainer("heitor", "Heitor", 3, 0, false, false, 2, List.of("drednaw level=26", "centiskorch level=27", "falinks level=27")));
        add(new Trainer("bruna", "Bruna", 4, 0, false, false, 2, List.of("corviknight level=36", "boltund level=36", "toxtricity level=37", "frosmoth level=37")));
        add(new Trainer("valentina", "Valentina", 5, 0, false, false, 3, List.of("hatterene level=46", "grimmsnarl level=46", "mrrime level=47", "dragapult level=48")));
        // --- lideres de ginasio (classe Ultra)
        add(new Trainer("milo", "Milo", 0, 1, true, false, 3, List.of("gossifleur level=22", "eldegoss level=25", "applin level=23")));
        add(new Trainer("nessa", "Nessa", 0, 1, true, false, 3, List.of("goldeen level=26", "arrokuda level=26", "drednaw level=29")));
        add(new Trainer("kabu", "Kabu", 0, 1, true, false, 3, List.of("ninetales level=30", "arcanine level=30", "centiskorch level=33")));
        add(new Trainer("bea", "Bea", 0, 1, true, false, 3, List.of("hitmontop level=33", "pangoro level=33", "sirfetchd level=36", "machamp level=36")));
        add(new Trainer("opal", "Opal", 0, 1, true, false, 3, List.of("alcremie level=36", "togekiss level=37", "grimmsnarl level=40")));
        add(new Trainer("melony", "Melony", 0, 1, true, false, 3, List.of("lapras level=40", "mamoswine level=40", "eiscue level=42", "frosmoth level=43")));
        add(new Trainer("piers", "Piers", 0, 1, true, false, 3, List.of("scrafty level=43", "malamar level=43", "skuntank level=45", "obstagoon level=47")));
        add(new Trainer("raihan", "Raihan", 0, 1, true, false, 3, List.of("gigalith level=46", "flygon level=46", "sandaconda level=49", "duraludon level=51")));
        // --- campeao (classe Master)
        add(new Trainer("leon", "Leon", 0, 2, false, true, 5, List.of("aegislash level=63", "dragapult level=63", "haxorus level=64", "seismitoad level=64", "mrrime level=65", "charizard level=68")));
    }

    public static Trainer byName(String displayName) {
        for (Trainer t : ALL.values()) {
            if (t.name().equals(displayName)) return t;
        }
        return null;
    }

    private Trainers() {}
}
