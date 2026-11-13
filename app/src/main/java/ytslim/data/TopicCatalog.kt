package ytslim.data

import androidx.annotation.StringRes
import ytslim.R

/**
 * Os tópicos sugeridos na tela de tópicos (primeiro uso e Settings). Desde a 1.0 o app não vem com
 * tópicos prontos: cada pessoa marca os seus aqui, para ver mais ou ver menos (pedido do Johan,
 * 2026-11-13). Os que ela cria à mão nos Ajustes ficam de fora desta lista e não são mexidos.
 *
 * As palavras-chave vêm em inglês, português e espanhol, qualquer que seja o idioma do app: os
 * vídeos chegam nas três línguas. Valem as mesmas regras de [TopicRules] (palavras inteiras, sem
 * acentos nem maiúsculas). Palavras curtas e ambíguas ficam de fora, para não pegar vídeo errado.
 *
 * [lessFirst]: os que costumam ir para "ver menos" aparecem primeiro nesse passo.
 */
enum class SuggestedTopic(@StringRes val label: Int, val lessFirst: Boolean, val keywords: List<String>) {
    DOCUMENTARIES(
        R.string.topic_documentaries, false,
        listOf("documentary", "documentaries", "documentario", "documentarios", "documental", "documentales", "docuseries"),
    ),
    VIDEO_ESSAYS(
        R.string.topic_video_essays, false,
        listOf("video essay", "video essays", "video ensaio", "ensaio em video", "video ensayo", "deep dive", "explained", "explicado", "explicada"),
    ),
    HISTORY(
        R.string.topic_history, false,
        listOf("history", "historical", "historia", "historico", "historica", "ancient", "antiga", "antigua", "medieval", "empire", "imperio", "civilization", "civilizacao", "civilizacion", "world war", "guerra mundial"),
    ),
    SCIENCE(
        R.string.topic_science, false,
        listOf("science", "scientific", "scientist", "ciencia", "ciencias", "cientifico", "cientista", "cientifica", "physics", "fisica", "chemistry", "quimica", "biology", "biologia", "experiment", "experimento"),
    ),
    SPACE(
        R.string.topic_space, false,
        listOf("space", "astronomy", "astronomia", "astrophysics", "astrofisica", "nasa", "spacex", "universe", "universo", "galaxy", "galaxia", "planet", "planeta", "black hole", "buraco negro", "agujero negro", "rocket", "foguete", "cohete", "telescope", "telescopio"),
    ),
    NATURE(
        R.string.topic_nature, false,
        listOf("nature", "natureza", "naturaleza", "wildlife", "vida selvagem", "vida salvaje", "animals", "animais", "animales", "ocean", "oceano", "forest", "floresta", "bosque", "selva", "birds", "aves", "safari"),
    ),
    LECTURES(
        R.string.topic_lectures, false,
        listOf("lecture", "lectures", "palestra", "palestras", "conferencia", "charla", "aula", "clase magistral", "keynote", "ted talk", "ted talks", "tedx", "seminar", "seminario", "masterclass"),
    ),
    INTERVIEWS(
        R.string.topic_interviews, false,
        listOf("interview", "interviews", "entrevista", "entrevistas", "podcast", "podcasts", "conversation", "conversa", "conversacion", "talk show", "bate papo"),
    ),
    PHILOSOPHY(
        R.string.topic_philosophy, false,
        listOf("philosophy", "philosopher", "filosofia", "filosofo", "stoicism", "estoicismo", "ethics", "etica", "existentialism", "existencialismo"),
    ),
    ENGINEERING(
        R.string.topic_engineering, false,
        listOf("engineering", "engineer", "engenharia", "engenheiro", "ingenieria", "ingeniero", "mechanical", "mecanica", "machining", "cnc", "electronics", "eletronica", "electronica", "circuit", "circuito", "pcb", "arduino", "robotics", "robotica", "robot"),
    ),
    TECHNOLOGY(
        R.string.topic_technology, false,
        listOf("technology", "tecnologia", "tech", "hardware", "semiconductor", "semicondutor", "chip", "chips", "gadget", "gadgets", "smartphone", "linux", "computer", "computador", "computadora"),
    ),
    PROGRAMMING(
        R.string.topic_programming, false,
        listOf("programming", "programacao", "programacion", "programar", "coding", "software", "developer", "desenvolvedor", "desarrollador", "python", "javascript", "typescript", "kotlin", "rust", "java", "c++", "algorithm", "algoritmo", "compiler", "compilador"),
    ),
    AI(
        R.string.topic_ai, false,
        listOf("ai", "artificial intelligence", "inteligencia artificial", "ia generativa", "machine learning", "aprendizado de maquina", "aprendizaje automatico", "deep learning", "neural network", "rede neural", "redes neurais", "red neuronal", "llm", "llms", "chatgpt", "openai", "anthropic", "claude", "gemini"),
    ),
    MATH(
        R.string.topic_math, false,
        listOf("math", "maths", "mathematics", "matematica", "matematicas", "geometry", "geometria", "calculus", "calculo", "algebra", "theorem", "teorema", "equation", "equacao", "ecuacion"),
    ),
    ECONOMICS(
        R.string.topic_economics, false,
        listOf("economics", "economy", "economia", "finance", "financas", "finanzas", "investing", "investimento", "investimentos", "inversion", "stock market", "bolsa de valores", "inflation", "inflacao", "inflacion", "money", "dinheiro", "dinero"),
    ),
    GEOGRAPHY(
        R.string.topic_geography, false,
        listOf("geography", "geografia", "geopolitics", "geopolitica", "maps", "mapas", "countries", "paises", "borders", "fronteiras", "fronteras"),
    ),
    AVIATION(
        R.string.topic_aviation, false,
        listOf("aviation", "aviacao", "aviacion", "airplane", "aircraft", "aviao", "avioes", "avion", "aviones", "pilot", "piloto", "boeing", "airbus", "cockpit", "flight", "voo", "vuelo"),
    ),
    CARS(
        R.string.topic_cars, false,
        listOf("car", "cars", "carro", "carros", "coche", "coches", "autos", "automotive", "automotivo", "supercar", "test drive"),
    ),
    DIY(
        R.string.topic_diy, false,
        listOf("diy", "do it yourself", "faca voce mesmo", "hazlo tu mismo", "bricolaje", "maker", "woodworking", "marcenaria", "carpinteria", "restoration", "restauracao", "restauracion", "workshop"),
    ),
    ARCHITECTURE(
        R.string.topic_architecture, false,
        listOf("architecture", "architect", "arquitetura", "arquitectura", "arquiteto", "arquitecto", "urbanism", "urbanismo", "city planning", "skyscraper", "arranha ceu", "rascacielos"),
    ),
    ART(
        R.string.topic_art, false,
        listOf("art", "arte", "artist", "artista", "painting", "pintura", "drawing", "desenho", "dibujo", "sculpture", "escultura", "design", "illustration", "ilustracao", "ilustracion"),
    ),
    COOKING(
        R.string.topic_cooking, false,
        listOf("cooking", "recipe", "recipes", "receita", "receitas", "receta", "recetas", "culinaria", "cocina", "cozinha", "chef", "baking", "food", "comida"),
    ),
    TRAVEL(
        R.string.topic_travel, false,
        listOf("travel", "travelling", "traveling", "viagem", "viagens", "viaje", "viajes", "trip", "road trip", "backpacking", "mochilao", "mochilero", "travel vlog"),
    ),
    MUSIC(
        R.string.topic_music, false,
        listOf("music", "musica", "musico", "concert", "concerto", "concierto", "live session", "album", "guitar", "guitarra", "violao", "piano", "orchestra", "orquestra", "orquesta", "song", "cancion", "cancao"),
    ),
    MOVIES(
        R.string.topic_movies, false,
        listOf("movie", "movies", "film", "films", "filme", "filmes", "pelicula", "peliculas", "cinema", "cine", "series", "tv series", "trailer"),
    ),
    BOOKS(
        R.string.topic_books, false,
        listOf("book", "books", "livro", "livros", "libro", "libros", "literature", "literatura", "novel", "reading", "leitura", "lectura", "author", "autor"),
    ),
    LANGUAGES(
        R.string.topic_languages, false,
        listOf("language learning", "learn english", "aprender ingles", "learn spanish", "aprender espanhol", "aprender espanol", "linguistics", "linguistica", "grammar", "gramatica", "idiomas", "polyglot", "poliglota"),
    ),
    HEALTH(
        R.string.topic_health, false,
        listOf("health", "saude", "salud", "fitness", "workout", "treino", "entrenamiento", "exercise", "exercicio", "ejercicio", "nutrition", "nutricao", "nutricion", "diet", "dieta", "corrida", "yoga", "meditation", "meditacao", "meditacion"),
    ),
    CHESS(
        R.string.topic_chess, false,
        listOf("chess", "xadrez", "ajedrez", "grandmaster", "grande mestre", "gran maestro", "magnus carlsen", "chess com", "lichess"),
    ),
    SLEEP(
        R.string.topic_sleep, false,
        listOf("sleep", "sleeping", "sleepy", "dormir", "sono", "sueno", "relaxing", "relaxante", "relajante", "calm", "calmo", "calma", "rain sounds", "sons de chuva", "sonidos de lluvia", "ambient", "white noise", "ruido branco", "ruido blanco", "bedtime"),
    ),
    COMEDY(
        R.string.topic_comedy, false,
        listOf("comedy", "comedia", "stand up", "standup", "humor", "funny", "engracado", "gracioso", "sketch", "esquete", "sketches"),
    ),
    GAMING(
        R.string.topic_gaming, true,
        listOf("gaming", "gameplay", "gamer", "video game", "video games", "videogame", "videogames", "videojuego", "videojuegos", "minecraft", "fortnite", "roblox", "gta", "playstation", "xbox", "nintendo", "speedrun", "lets play", "walkthrough", "jogatina"),
    ),
    ANIME(
        R.string.topic_anime, true,
        listOf("anime", "animes", "manga", "otaku", "one piece", "naruto", "dragon ball"),
    ),
    ASMR(
        R.string.topic_asmr, true,
        listOf("asmr", "tingles", "whisper", "whispering", "sussurro", "susurro", "susurros"),
    ),
    NEWS(
        R.string.topic_news, true,
        listOf("news", "breaking news", "noticia", "noticias", "jornal", "telejornal", "noticiero", "ultima hora", "urgente", "headlines", "manchetes", "titulares", "live news"),
    ),
    POLITICS(
        R.string.topic_politics, true,
        listOf("politics", "political", "politica", "politico", "election", "elections", "eleicao", "eleicoes", "eleccion", "elecciones", "president", "presidente", "congress", "congresso", "congreso", "senate", "senado", "senador", "deputado", "diputado", "government", "governo", "gobierno", "democrats", "republicans", "trump", "biden", "lula", "bolsonaro", "milei", "white house", "planalto"),
    ),
    SPORTS(
        R.string.topic_sports, true,
        listOf("sports", "sport", "esporte", "esportes", "deporte", "deportes", "football", "soccer", "futebol", "futbol", "nba", "nfl", "mlb", "nhl", "ufc", "mma", "fifa", "world cup", "copa do mundo", "copa del mundo", "champions league", "premier league", "la liga", "brasileirao", "libertadores", "formula 1", "f1", "olympics", "olimpiadas", "basketball", "basquete", "baloncesto", "tennis", "tenis", "highlights", "melhores momentos", "golazo", "gols", "goles"),
    ),
    CELEBRITIES(
        R.string.topic_celebrities, true,
        listOf("celebrity", "celebrities", "celebridade", "celebridades", "famosos", "famosas", "gossip", "fofoca", "fofocas", "chisme", "chismes", "farandula", "hollywood", "kardashian", "red carpet", "tapete vermelho", "alfombra roja"),
    ),
    REACTIONS(
        R.string.topic_reactions, true,
        listOf("react", "reacts", "reaction", "reactions", "reacting", "reagindo", "reacao", "reacoes", "reage", "reaccion", "reacciones", "reaccionando", "reacciona"),
    ),
    PRANKS(
        R.string.topic_pranks, true,
        listOf("prank", "pranks", "pegadinha", "pegadinhas", "trollagem", "broma", "bromas", "last to leave"),
    ),
    CRYPTO(
        R.string.topic_crypto, true,
        listOf("crypto", "cryptocurrency", "cripto", "criptomoeda", "criptomoedas", "criptomoneda", "criptomonedas", "bitcoin", "btc", "ethereum", "eth", "nft", "nfts", "altcoin", "altcoins", "memecoin", "web3", "solana", "dogecoin"),
    ),
    TRUE_CRIME(
        R.string.topic_true_crime, true,
        listOf("true crime", "crime", "crimes", "crimen", "crimenes", "murder", "murders", "assassinato", "asesinato", "serial killer", "assassino", "asesino", "unsolved", "caso real", "investigation", "investigacao", "investigacion"),
    ),
    BEAUTY(
        R.string.topic_beauty, true,
        listOf("beauty", "beleza", "belleza", "makeup", "maquiagem", "maquillaje", "skincare", "skin care", "cuidados com a pele", "cuidado de la piel", "fashion", "moda", "outfit", "outfits", "lookbook", "grwm", "get ready with me", "haul", "hauls", "try on"),
    ),
    UNBOXING(
        R.string.topic_unboxing, true,
        listOf("unboxing", "unbox", "unboxings", "desembalando", "abrindo a caixa", "abriendo", "mystery box", "caixa misteriosa", "caja misteriosa"),
    ),
    KIDS(
        R.string.topic_kids, true,
        listOf("kids", "kid", "children", "crianca", "criancas", "infantil", "ninos", "nino", "nursery rhymes", "cancoes infantis", "canciones infantiles", "cartoon", "cartoons", "desenho animado", "desenhos animados", "dibujos animados", "toys", "brinquedos", "juguetes", "cocomelon", "peppa pig"),
    ),
    ;

    /** O id gravado no tópico ([Topic.id]): o nome do enum, que nunca muda. */
    val id: String get() = name

    fun topic(name: String): Topic = Topic(name, keywords, id)

    companion object {
        fun byId(id: String?): SuggestedTopic? = id?.let { wanted -> entries.firstOrNull { it.id == wanted } }

        /** A ordem do passo "ver mais": a de cima, pensada para quem vê vídeos longos. */
        val forMore: List<SuggestedTopic> get() = entries

        /** A ordem do passo "ver menos": o que as pessoas costumam tirar vem primeiro. */
        val forLess: List<SuggestedTopic> get() = entries.filter { it.lessFirst } + entries.filterNot { it.lessFirst }
    }
}
