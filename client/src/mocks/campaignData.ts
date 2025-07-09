import type { Donor, Message, Campaign } from "../types/types";
import type { ChatStatus } from "../types/index";

// Campanhas disponíveis
export const mockCampaigns: Campaign[] = [
  {
    id: "camp_1",
    name: "Campanha Junho 2025",
    description: "Campanha especial para reposição de estoque",
    startDate: "2025-06-01",
    endDate: "2025-06-30",
    status: "active",
    color: "#3b82f6",
    templatesUsed: [] // Templates serão carregados da API
  },
  {
    id: "camp_2", 
    name: "Urgência O-",
    description: "Campanha emergencial para tipo sanguíneo O-",
    startDate: "2025-06-15",
    endDate: "2025-06-25",
    status: "active",
    color: "#dc2626",
    templatesUsed: [] // Templates serão carregados da API
  },
  {
    id: "camp_3",
    name: "Doadores Corporativos",
    description: "Campanha voltada para empresas parceiras",
    startDate: "2025-05-01",
    endDate: "2025-07-31",
    status: "active", 
    color: "#059669",
    templatesUsed: [] // Templates serão carregados da API
  },
  {
    id: "camp_4",
    name: "Primeira Doação",
    description: "Campanha de incentivo a novos doadores",
    startDate: "2025-06-01",
    endDate: "2025-08-31",
    status: "active",
    color: "#7c3aed",
    templatesUsed: [] // Templates serão carregados da API
  },
  {
    id: "camp_5",
    name: "Retorno Maio",
    description: "Campanha finalizada para doadores de retorno",
    startDate: "2025-05-01",
    endDate: "2025-05-31",
    status: "completed",
    color: "#6b7280",
    templatesUsed: [] // Templates serão carregados da API
  }
];

// Conversas por campanha - Campanha Junho 2025
export const mockDonorsCamp1: Donor[] = [
  {
    id: "camp1_ativo_1",
    name: "Maria Silva",
    lastMessage: "Olá! Gostaria de agendar uma doação para esta semana. Tenho disponibilidade na terça e quarta à tarde.",
    timestamp: "14:30",
    unread: 1,
    status: "online",
    bloodType: "O+",
    phone: "(11) 99999-9999",
    email: "maria.silva@email.com",
    lastDonation: "12/03/2025",
    totalDonations: 8,
    address: "Rua das Flores, 123 - São Paulo, SP",
    birthDate: "15/08/1985",
    weight: 65,
    height: 165,
    hasActiveConversation: true,
    campaignId: "camp_1",
    campaigns: ["camp_1", "camp_3"],
  },
  {
    id: "camp1_ativo_2", 
    name: "João Santos",
    lastMessage: "Oi Sofia! Preciso reagendar minha doação porque surgiu um compromisso. Você pode me ajudar?",
    timestamp: "13:45",
    unread: 1,
    status: "online",
    bloodType: "A-",
    phone: "(11) 88888-8888", 
    email: "joao.santos@email.com",
    lastDonation: "08/02/2025",
    totalDonations: 15,
    address: "Av. Paulista, 456 - São Paulo, SP",
    birthDate: "22/03/1990",
    weight: 75,
    height: 178,
    hasActiveConversation: true,
    campaignId: "camp_1",
    campaigns: ["camp_1"],
  },
  {
    id: "camp1_ativo_3",
    name: "Lucas Mendes", 
    lastMessage: "Ótimo! Posso ir na sexta-feira de manhã. Que documentos preciso levar?",
    timestamp: "12:10",
    unread: 1,
    status: "online",
    bloodType: "A+",
    phone: "(11) 77777-7777",
    email: "lucas.mendes@email.com",
    lastDonation: "Nunca doou",
    totalDonations: 0,
    address: "Rua das Palmeiras, 456 - São Paulo, SP",
    birthDate: "12/09/1998",
    weight: 70,
    height: 175,
    hasActiveConversation: true,
    campaignId: "camp_1",
    campaigns: ["camp_1"],
  },
  {
    id: "camp1_aguardando_1",
    name: "Carla Ferreira",
    lastMessage: "Perfeito! Agendei sua doação para 21/06/2025 às 09:00. Confirma presença? 📅",
    timestamp: "16:45",
    unread: 0,
    status: "offline",
    bloodType: "AB+",
    phone: "(11) 55555-5555",
    email: "carla.ferreira@email.com",
    lastDonation: "15/01/2025",
    totalDonations: 3,
    address: "Rua Consolação, 789 - São Paulo, SP",
    birthDate: "10/12/1992",
    weight: 58,
    height: 162,
    hasActiveConversation: true,
    campaignId: "camp_1",
    campaigns: ["camp_1"],
  },
  {
    id: "camp1_aguardando_2",
    name: "Roberto Costa",
    lastMessage: "Obrigado pelas informações! Vou pensar e entro em contato.",
    timestamp: "09:30", 
    unread: 0,
    status: "offline",
    bloodType: "B-",
    phone: "(11) 44444-4444",
    email: "roberto.costa@email.com",
    lastDonation: "05/05/2025",
    totalDonations: 7,
    address: "Av. Rebouças, 321 - São Paulo, SP",
    birthDate: "28/02/1987",
    weight: 82,
    height: 183,
    hasActiveConversation: true,
    campaignId: "camp_1",
    campaigns: ["camp_1"],
  },
  // Contatos Inativos da Campanha Junho 2025
  {
    id: "camp1_inativo_1",
    name: "Sandra Oliveira",
    lastMessage: "Não tenho interesse no momento, obrigada.",
    timestamp: "Ontem",
    unread: 0,
    status: "offline",
    bloodType: "AB+",
    phone: "(11) 99998-8887",
    email: "sandra.oliveira@email.com",
    lastDonation: "15/12/2024",
    totalDonations: 2,
    address: "Rua dos Bandeirantes, 789 - São Paulo, SP",
    birthDate: "10/05/1993",
    weight: 60,
    height: 160,
    hasActiveConversation: false,
    campaignId: "camp_1",
    campaigns: ["camp_1"],
  },
  {
    id: "camp1_inativo_2",
    name: "Felipe Martinez",
    lastMessage: "Estou viajando este mês, talvez em julho consiga.",
    timestamp: "3 dias",
    unread: 0,
    status: "offline",
    bloodType: "O-",
    phone: "(11) 77776-6665",
    email: "felipe.martinez@email.com",
    lastDonation: "22/01/2025",
    totalDonations: 5,
    address: "Av. Ibirapuera, 1234 - São Paulo, SP",
    birthDate: "08/11/1989",
    weight: 78,
    height: 182,
    hasActiveConversation: false,
    campaignId: "camp_1",
    campaigns: ["camp_1"],
  },
  {
    id: "camp1_inativo_3",
    name: "Patricia Nunes",
    lastMessage: "Muito obrigada pelas informações. Vou avaliar com meu médico primeiro.",
    timestamp: "1 sem",
    unread: 0,
    status: "offline",
    bloodType: "A+",
    phone: "(11) 66665-5554",
    email: "patricia.nunes@email.com",
    lastDonation: "Nunca doou",
    totalDonations: 0,
    address: "Rua da Consolação, 567 - São Paulo, SP",
    birthDate: "14/07/1996",
    weight: 55,
    height: 158,
    hasActiveConversation: false,
    campaignId: "camp_1",
    campaigns: ["camp_1"],
  }
];

// Conversas por campanha - Urgência O-
export const mockDonorsCamp2: Donor[] = [
  {
    id: "camp2_ativo_1",
    name: "Pedro Oliveira",
    lastMessage: "Urgência mesmo? Posso vir hoje à tarde se for muito necessário!",
    timestamp: "15:20",
    unread: 1,
    status: "online",
    bloodType: "O-",
    phone: "(11) 66666-6666",
    email: "pedro.oliveira@email.com",
    lastDonation: "02/04/2025",
    totalDonations: 12,
    address: "Rua Augusta, 321 - São Paulo, SP",
    birthDate: "05/07/1988",
    weight: 80,
    height: 185,
    hasActiveConversation: true,
    campaignId: "camp_2",
    campaigns: ["camp_2"],
  },
  {
    id: "camp2_ativo_2",
    name: "Fernanda Lima",
    lastMessage: "Olá! Sou O- também. Vi a mensagem de urgência. Quando posso ir?",
    timestamp: "14:50",
    unread: 1,
    status: "online",
    bloodType: "O-",
    phone: "(11) 44444-4444",
    email: "fernanda.lima@email.com",
    lastDonation: "28/03/2025",
    totalDonations: 6,
    address: "Av. Brasil, 654 - São Paulo, SP",
    birthDate: "18/11/1995",
    weight: 62,
    height: 168,
    hasActiveConversation: true,
    campaignId: "camp_2",
    campaigns: ["camp_2"],
  },
  {
    id: "camp2_aguardando_1",
    name: "Ana Paula",
    lastMessage: "Muito obrigada! Agendamento confirmado para amanhã às 14h.",
    timestamp: "13:20",
    unread: 0,
    status: "offline",
    bloodType: "O-",
    phone: "(11) 33333-3333",
    email: "ana.paula@email.com",
    lastDonation: "10/05/2025",
    totalDonations: 4,
    address: "Rua Oscar Freire, 789 - São Paulo, SP",
    birthDate: "03/01/1991",
    weight: 59,
    height: 164,
    hasActiveConversation: true,
    campaignId: "camp_2",
    campaigns: ["camp_2"],
  },
  // Contatos Inativos da Urgência O-
  {
    id: "camp2_inativo_1",
    name: "Carlos Eduardo",
    lastMessage: "Não posso doar agora, estou fazendo tratamento médico.",
    timestamp: "2 dias",
    unread: 0,
    status: "offline",
    bloodType: "O-",
    phone: "(11) 55554-4443",
    email: "carlos.eduardo@email.com",
    lastDonation: "18/04/2025",
    totalDonations: 18,
    address: "Rua São João, 987 - São Paulo, SP",
    birthDate: "12/02/1982",
    weight: 88,
    height: 190,
    hasActiveConversation: false,
    campaignId: "camp_2",
    campaigns: ["camp_2"],
  },
  {
    id: "camp2_inativo_2",
    name: "Beatriz Santos",
    lastMessage: "Obrigada pelo contato, mas não tenho disponibilidade esta semana.",
    timestamp: "4 dias",
    unread: 0,
    status: "offline",
    bloodType: "O-",
    phone: "(11) 44443-3332",
    email: "beatriz.santos@email.com",
    lastDonation: "05/03/2025",
    totalDonations: 7,
    address: "Av. Atlântica, 456 - São Paulo, SP",
    birthDate: "28/09/1990",
    weight: 63,
    height: 167,
    hasActiveConversation: false,
    campaignId: "camp_2",
    campaigns: ["camp_2"],
  }
];

// Conversas por campanha - Doadores Corporativos
export const mockDonorsCamp3: Donor[] = [
  {
    id: "camp3_ativo_1",
    name: "Roberto Silva (TechCorp)",
    lastMessage: "Boa tarde! Nossa empresa tem interesse sim. Quantas pessoas vocês conseguem atender por dia?",
    timestamp: "11:30",
    unread: 1,
    status: "online",
    bloodType: "B+",
    phone: "(11) 33333-3333",
    email: "roberto.silva@techcorp.com",
    lastDonation: "10/04/2025",
    totalDonations: 20,
    address: "Av. Faria Lima, 987 - São Paulo, SP",
    birthDate: "25/02/1980",
    weight: 85,
    height: 180,
    hasActiveConversation: true,
    campaignId: "camp_3",
    campaigns: ["camp_3", "camp_1"],
  },
  {
    id: "camp3_ativo_2",
    name: "Mariana Santos (FinTech)",
    lastMessage: "Perfeito! Nossa equipe de RH já está organizando. Podemos agendar para próxima terça?",
    timestamp: "10:45",
    unread: 1,
    status: "online",
    bloodType: "A-",
    phone: "(11) 22222-2222",
    email: "mariana.santos@fintech.com",
    lastDonation: "20/04/2025",
    totalDonations: 12,
    address: "Av. Paulista, 1500 - São Paulo, SP",
    birthDate: "14/06/1985",
    weight: 68,
    height: 170,
    hasActiveConversation: true,
    campaignId: "camp_3",
    campaigns: ["camp_3"],
  },
  {
    id: "camp3_aguardando_1",
    name: "Carlos Rodrigues (StartupXYZ)",
    lastMessage: "Vou conversar com a diretoria e confirmo na segunda-feira.",
    timestamp: "14:20",
    unread: 0,
    status: "offline",
    bloodType: "AB-",
    phone: "(11) 11111-1111",
    email: "carlos.rodrigues@startupxyz.com",
    lastDonation: "Nunca doou",
    totalDonations: 0,
    address: "Rua dos Três Irmãos, 500 - São Paulo, SP",
    birthDate: "22/08/1988",
    weight: 75,
    height: 178,
    hasActiveConversation: true,
    campaignId: "camp_3",
    campaigns: ["camp_3"],
  },
  // Contatos Inativos dos Doadores Corporativos
  {
    id: "camp3_inativo_1",
    name: "Julia Costa (ConsultCorp)",
    lastMessage: "Nossa empresa não tem interesse no momento, mas agradecemos o contato.",
    timestamp: "1 sem",
    unread: 0,
    status: "offline",
    bloodType: "B+",
    phone: "(11) 99887-7766",
    email: "julia.costa@consultcorp.com",
    lastDonation: "Nunca doou",
    totalDonations: 0,
    address: "Av. Berrini, 2000 - São Paulo, SP",
    birthDate: "05/04/1987",
    weight: 67,
    height: 172,
    hasActiveConversation: false,
    campaignId: "camp_3",
    campaigns: ["camp_3"],
  }
];

// Conversas por campanha - Primeira Doação
export const mockDonorsCamp4: Donor[] = [
  {
    id: "camp4_ativo_1",
    name: "Giovana Almeida",
    lastMessage: "Oi! Nunca doei sangue antes. Vocês podem me explicar como funciona?",
    timestamp: "16:15",
    unread: 1,
    status: "online",
    bloodType: "A+",
    phone: "(11) 88877-6655",
    email: "giovana.almeida@email.com",
    lastDonation: "Nunca doou",
    totalDonations: 0,
    address: "Rua das Palmeiras, 123 - São Paulo, SP",
    birthDate: "25/03/1999",
    weight: 58,
    height: 165,
    hasActiveConversation: true,
    campaignId: "camp_4",
    campaigns: ["camp_4"],
  },
  {
    id: "camp4_ativo_2",
    name: "Bruno Ferreira",
    lastMessage: "Tenho 18 anos, posso doar? Quais são os requisitos?",
    timestamp: "15:45",
    unread: 1,
    status: "online",
    bloodType: "O+",
    phone: "(11) 77766-5544",
    email: "bruno.ferreira@email.com",
    lastDonation: "Nunca doou",
    totalDonations: 0,
    address: "Av. Paulista, 2000 - São Paulo, SP",
    birthDate: "10/01/2006",
    weight: 72,
    height: 180,
    hasActiveConversation: true,
    campaignId: "camp_4",
    campaigns: ["camp_4"],
  },
  {
    id: "camp4_aguardando_1",
    name: "Leticia Souza",
    lastMessage: "Perfeito! Agendei para sábado às 10h. Muito obrigada!",
    timestamp: "12:30",
    unread: 0,
    status: "offline",
    bloodType: "B-",
    phone: "(11) 66655-4433",
    email: "leticia.souza@email.com",
    lastDonation: "Nunca doou",
    totalDonations: 0,
    address: "Rua Augusta, 567 - São Paulo, SP",
    birthDate: "18/06/2001",
    weight: 54,
    height: 162,
    hasActiveConversation: true,
    campaignId: "camp_4",
    campaigns: ["camp_4"],
  },
  {
    id: "camp4_inativo_1",
    name: "Diego Santos",
    lastMessage: "Ainda tenho receio. Vou pensar melhor e talvez entre em contato mais tarde.",
    timestamp: "5 dias",
    unread: 0,
    status: "offline",
    bloodType: "AB+",
    phone: "(11) 55544-3322",
    email: "diego.santos@email.com",
    lastDonation: "Nunca doou",
    totalDonations: 0,
    address: "Rua da Liberdade, 890 - São Paulo, SP",
    birthDate: "14/11/2002",
    weight: 68,
    height: 175,
    hasActiveConversation: false,
    campaignId: "camp_4",
    campaigns: ["camp_4"],
  }
];

// Conversas por campanha - Retorno Maio (Finalizada)
export const mockDonorsCamp5: Donor[] = [
  {
    id: "camp5_ativo_1",
    name: "Ricardo Lima",
    lastMessage: "Olá! Gostaria de agendar minha próxima doação. Quando posso ir?",
    timestamp: "11:20",
    unread: 1,
    status: "online",
    bloodType: "O+",
    phone: "(11) 44433-2211",
    email: "ricardo.lima@email.com",
    lastDonation: "15/05/2025",
    totalDonations: 25,
    address: "Av. Ipiranga, 1234 - São Paulo, SP",
    birthDate: "30/08/1975",
    weight: 85,
    height: 178,
    hasActiveConversation: true,
    campaignId: "camp_5",
    campaigns: ["camp_5", "camp_1"],
  },
  {
    id: "camp5_aguardando_1",
    name: "Amanda Costa",
    lastMessage: "Obrigada! Vou no horário agendado mesmo.",
    timestamp: "10:15",
    unread: 0,
    status: "offline",
    bloodType: "A-",
    phone: "(11) 33322-1100",
    email: "amanda.costa@email.com",
    lastDonation: "28/05/2025",
    totalDonations: 12,
    address: "Rua Bela Vista, 456 - São Paulo, SP",
    birthDate: "22/04/1988",
    weight: 61,
    height: 168,
    hasActiveConversation: true,
    campaignId: "camp_5",
    campaigns: ["camp_5"],
  },
  {
    id: "camp5_inativo_1",
    name: "Marcos Pereira",
    lastMessage: "Já doei este mês na campanha anterior. Obrigado pelo lembrete!",
    timestamp: "1 sem",
    unread: 0,
    status: "offline",
    bloodType: "B+",
    phone: "(11) 22211-0099",
    email: "marcos.pereira@email.com",
    lastDonation: "20/05/2025",
    totalDonations: 30,
    address: "Av. Brigadeiro, 789 - São Paulo, SP",
    birthDate: "12/01/1978",
    weight: 90,
    height: 185,
    hasActiveConversation: false,
    campaignId: "camp_5",
    campaigns: ["camp_5", "camp_1", "camp_3"],
  }
];

// Mensagens por campanha
export const messagesByCampaign: Record<string, Record<string, Message[]>> = {
  camp_1: {
    "camp1_ativo_1": [
      {
        id: "msg_c1_1_1",
        senderId: "ai",
        content: "Olá Maria! 😊 Sou Sofia, assistente do Centro de Sangue de São Paulo. Esperamos que você esteja bem! Estamos com nossa Campanha Junho 2025 em andamento e nossos estoques de sangue tipo O+ estão em níveis críticos. Sabemos que você é uma doadora experiente e sempre muito generosa conosco. Gostaria de agendar uma nova doação? Temos horários disponíveis esta semana e a próxima. Sua contribuição faz toda a diferença para salvar vidas! ❤️",
        timestamp: "14:25",
        isAI: true,
        campaignId: "camp_1",
      },
      {
        id: "msg_c1_1_2",
        senderId: "camp1_ativo_1",
        content: "Oi Sofia! Claro, sempre fico feliz em ajudar. Gostaria muito de agendar uma doação para esta semana. Tenho disponibilidade na terça-feira à tarde, a partir das 14h, ou na quarta-feira o dia todo. Prefiro vir na quarta de manhã se possível, por volta das 9h ou 10h. É preciso fazer jejum ou algum preparo especial? Também gostaria de saber se vocês ainda estão precisando mais do meu tipo sanguíneo ou se posso indicar alguns amigos que também querem doar.",
        timestamp: "14:30",
        isAI: false,
        campaignId: "camp_1",
      },
      {
        id: "msg_c1_1_3",
        senderId: "ai",
        content: "Que maravilha, Maria! 🌟 Fico muito feliz com sua disposição. Vou agendar para quarta-feira às 9h30, que tal? É um horário excelente! Sobre o preparo: é importante ter uma boa noite de sono, tomar um café da manhã leve (evite alimentos gordurosos), estar bem hidratada e não ter consumido bebidas alcoólicas nas últimas 12h. Ah, e sobre o tipo O+: sim, estamos com grande necessidade! E seria fantástico se você pudesse indicar seus amigos. Temos até um programa de indicação onde você ganha pontos para trocar por brindes. Posso enviar o link para eles se inscreverem?",
        timestamp: "14:35",
        isAI: true,
        campaignId: "camp_1",
      }
    ],
    "camp1_ativo_2": [
      {
        id: "msg_c1_2_1",
        senderId: "ai",
        content: "Oi João! Esperamos que esteja tudo bem com você! 😊 Notei que já faz mais de 3 meses desde sua última doação aqui no Centro de Sangue. Sabemos que você é um doador muito comprometido (15 doações! 👏) e sempre nos ajudou muito. Nossa Campanha Junho está em andamento e estamos precisando especialmente de sangue tipo A-, que sabemos ser o seu. Que tal agendar uma nova doação? Temos vários horários livres esta semana e podemos até oferecer um horário mais flexível para se adequar à sua agenda. Cada gesto seu salva até 4 vidas! ❤️",
        timestamp: "13:40",
        isAI: true,
        campaignId: "camp_1",
      },
      {
        id: "msg_c1_2_2",
        senderId: "camp1_ativo_2",
        content: "Oi Sofia! Tudo bem sim, obrigado por perguntar! Realmente quero doar novamente, inclusive já estava pensando nisso. Na verdade, eu tinha agendado para quinta-feira passada, mas acabei tendo um compromisso de trabalho urgente e não consegui avisar a tempo. Me sinto mal por isso! 😔 Preciso reagendar minha doação, mas dessa vez quero garantir que não vou precisar desmarcar. Você pode me ajudar a encontrar um horário que seja mais garantido? Prefiro de manhã, pois à tarde sempre surgem reuniões inesperadas no trabalho.",
        timestamp: "13:45",
        isAI: false,
        campaignId: "camp_1",
      },
      {
        id: "msg_c1_2_3",
        senderId: "ai",
        content: "João, não se preocupe! Essas coisas acontecem e entendemos perfeitamente. O importante é você querer remarcar! 😊 Vou agendar para um horário matinal então. Que tal segunda-feira às 8h? É o primeiro horário do dia, então você consegue resolver logo cedo antes do trabalho. Ou se preferir terça às 8h30. Também posso colocar um lembrete no seu WhatsApp na véspera, assim você não esquece. E não se sinta mal - o que importa é sua intenção de ajudar! Aliás, obrigada por ser tão dedicado. 15 doações são um exemplo inspirador! 🌟",
        timestamp: "13:48",
        isAI: true,
        campaignId: "camp_1",
      }
    ],
    "camp1_ativo_3": [
      {
        id: "msg_c1_3_1",
        senderId: "ai",
        content: "Olá Lucas! Muito obrigada pelo seu interesse em se tornar um doador de sangue! 😊 Vejo aqui que você ainda não tem histórico de doações conosco, então será sua primeira vez - que emocionante! Nossa Campanha Junho 2025 está focada especialmente em atrair novos doadores como você. Queremos garantir que sua experiência seja a melhor possível. Temos um atendimento especial para primeiros doadores, com acompanhamento personalizado e todas as orientações necessárias. O processo todo demora cerca de 45-60 minutos, incluindo a triagem médica. Gostaria de saber mais detalhes sobre como funciona?",
        timestamp: "12:05",
        isAI: true,
        campaignId: "camp_1",
      },
      {
        id: "msg_c1_3_2",
        senderId: "camp1_ativo_3",
        content: "Ótimo! Fico feliz em saber que vocês têm esse cuidado especial. Sempre quis doar, mas confesso que tinha um pouco de receio por não saber como era. Agora me sinto mais seguro! Posso ir na sexta-feira de manhã, por volta das 10h? Que documentos preciso levar? Também gostaria de saber se posso comer normalmente antes ou se precisa de algum preparo especial. Ah, e quero confirmar: é verdade que uma doação pode salvar até 4 vidas? Vi isso em algum lugar e fiquei impressionado!",
        timestamp: "12:10",
        isAI: false,
        campaignId: "camp_1",
      },
      {
        id: "msg_c1_3_3",
        senderId: "ai",
        content: "Lucas, que bom que você está se sentindo mais seguro! 🌟 É exatamente assim que queremos que todos se sintam. Sexta às 10h é perfeito! Sobre os documentos: traga um documento oficial com foto (RG, CNH ou passaporte). Sobre alimentação: tome um café da manhã normal, mas evite alimentos muito gordurosos 4h antes. E sim! É verdade sobre as 4 vidas - sua doação é separada em diferentes componentes (glóbulos vermelhos, plaquetas, plasma) que ajudam pacientes diferentes. Você será literalmente um herói! Vou mandar por WhatsApp um guia completo para primeira doação. Alguma dúvida sobre saúde ou medicamentos?",
        timestamp: "12:15",
        isAI: true,
        campaignId: "camp_1",
      }
    ],
    "camp1_aguardando_1": [
      {
        id: "msg_c1_ag1_1",
        senderId: "ai",
        content: "Olá Carla! Como está você? Esperamos que esteja tudo bem! 😊 Notei que faz alguns meses desde sua última doação aqui no Centro. Sabemos que você sempre foi uma doadora muito dedicada e suas 3 doações anteriores foram fundamentais para ajudar nossos pacientes. Nossa Campanha Junho 2025 está em andamento e estamos precisando especialmente de sangue AB+, que é exatamente o seu tipo! É um tipo sanguíneo muito especial e relativamente raro. Gostaria de agendar uma nova doação? Temos vários horários disponíveis esta semana e podemos ser flexíveis com seus horários.",
        timestamp: "16:40",
        isAI: true,
        campaignId: "camp_1",
      },
      {
        id: "msg_c1_ag1_2",
        senderId: "camp1_aguardando_1",
        content: "Oi! Tudo bem sim, obrigada por perguntar! Fico feliz em saber que vocês precisam do meu tipo sanguíneo. Realmente quero doar novamente, inclusive já estava pensando nisso há algumas semanas. Gostaria muito de agendar para esta semana se possível. Tenho preferência por manhãs, pois trabalho à tarde. Qual é o melhor dia para vocês? E queria saber também: como estão os estoques em geral? Vocês estão precisando muito ou é só uma campanha de rotina?",
        timestamp: "16:42",
        isAI: false,
        campaignId: "camp_1",
      },
      {
        id: "msg_c1_ag1_3",
        senderId: "ai",
        content: "Que bom saber que você já estava pensando em doar! 🌟 Os estoques estão realmente baixos, especialmente para AB+. Não é só campanha de rotina - estamos com necessidade real. Que tal quinta-feira às 9h? É um horário tranquilo e você sai cedo para trabalhar. Sobre os estoques: estamos com cerca de 3 dias de reserva quando o ideal seriam 7 dias. Sua doação AB+ é especialmente importante porque pode ajudar qualquer pessoa do sistema ABO. Vou agendar então?",
        timestamp: "16:44",
        isAI: true,
        campaignId: "camp_1",
      },
      {
        id: "msg_c1_ag1_4",
        senderId: "camp1_aguardando_1",
        content: "Nossa, não sabia que o AB+ podia ajudar qualquer pessoa! Isso me deixa ainda mais motivada. Quinta-feira às 9h está perfeito! Pode agendar sim.",
        timestamp: "16:45",
        isAI: false,
        campaignId: "camp_1",
      },
      {
        id: "msg_c1_ag1_5",
        senderId: "ai",
        content: "Perfeito! ✅ Agendamento confirmado para quinta-feira, 21/06/2025 às 09:00. Carla, você estará literalmente salvando vidas! Vou enviar um lembrete na véspera. Confirma presença? 📅",
        timestamp: "16:45",
        isAI: true,
        campaignId: "camp_1",
      }
    ],
    "camp1_aguardando_2": [
      {
        id: "msg_c1_ag2_1",
        senderId: "ai",
        content: "Bom dia, Roberto! Esperamos que esteja bem! 😊 Entramos em contato porque você é um dos nossos doadores mais experientes (7 doações!) e sempre demonstrou muito interesse em ajudar. Nossa Campanha Junho 2025 está com foco especial em doadores regulares como você. Estamos com estoques baixos de sangue B- e sabemos que esse é seu tipo. Temos várias novas datas disponíveis para doação esta semana e a próxima. Gostaria de agendar? Podemos ser flexíveis com horários para se adequar à sua agenda. Sua contribuição sempre fez muita diferença para nossos pacientes!",
        timestamp: "09:25",
        isAI: true,
        campaignId: "camp_1",
      },
      {
        id: "msg_c1_ag2_2",
        senderId: "camp1_aguardando_2",
        content: "Bom dia! Obrigado pelo contato e pelas informações detalhadas. Realmente tenho interesse em continuar doando, mas confesso que ando com a agenda muito corrida no trabalho. Preciso de um tempo para organizar minha agenda e ver quando consigo encaixar. Vocês enviam as orientações por email mesmo? E qual é o prazo ideal entre doações para homens? Quero ter certeza de que já posso doar novamente. Vou verificar minha agenda e entro em contato em breve para agendar, pode ser?",
        timestamp: "09:30",
        isAI: false,
        campaignId: "camp_1",
      },
      {
        id: "msg_c1_ag2_3",
        senderId: "ai",
        content: "Claro, Roberto! Entendemos perfeitamente sobre a agenda corrida. O prazo para homens é de 60 dias entre doações, e vejo que sua última foi em 05/05, então você já pode doar! 📋 Sim, enviamos tudo por email, inclusive com lembretes. Não se preocupe com pressa - quando conseguir encaixar na agenda, é só nos avisar. Estamos aqui para facilitar para você! Muito obrigada pela dedicação todos esses anos. 🙏",
        timestamp: "09:35",
        isAI: true,
        campaignId: "camp_1",
      }
    ],
    "camp1_inativo_1": [
      {
        id: "msg_c1_in1_1",
        senderId: "ai",
        content: "Olá Sandra! Como está você? Esperamos que esteja tudo bem! 😊 Estamos entrando em contato porque nossa Campanha Junho 2025 está em andamento e estamos precisando muito de doadores. Vejo aqui que você já fez 2 doações conosco anteriormente, o que mostra que você já conhece a importância desse gesto. Sabemos que sua última doação foi em dezembro/2024 e que você tem sangue AB+, que é um tipo muito especial e necessário. Gostaria de participar da nossa campanha atual? Podemos agendar um horário conveniente para você. Seria uma ajuda muito valiosa! ❤️",
        timestamp: "Ontem 14:30",
        isAI: true,
        campaignId: "camp_1",
      },
      {
        id: "msg_c1_in1_2",
        senderId: "camp1_inativo_1",
        content: "Oi! Obrigada pelo contato e por lembrarem de mim. Realmente sei da importância da doação e já doei antes, como vocês mencionaram. Porém, no momento não tenho interesse em doar novamente. Estou passando por um período meio complicado pessoalmente e prefiro não me comprometer com agendamentos. Quando eu estiver melhor e com mais disponibilidade, eu mesma entro em contato com vocês. Obrigada pela compreensão! 😊",
        timestamp: "Ontem 14:35",
        isAI: false,
        campaignId: "camp_1",
      },
      {
        id: "msg_c1_in1_3",
        senderId: "ai",
        content: "Entendemos perfeitamente, Sandra! 🤗 Agradecemos muito sua honestidade e respeitamos totalmente seu momento atual. O importante é que você cuide de si mesma primeiro. Quando se sentir melhor e pronta, estaremos aqui de braços abertos! Muito obrigada pelas doações anteriores - elas fizeram toda diferença. Desejamos que tudo se resolva da melhor forma para você! 💙",
        timestamp: "Ontem 14:40",
        isAI: true,
        campaignId: "camp_1",
      }
    ],
    "camp1_inativo_2": [
      {
        id: "msg_c1_in2_1",
        senderId: "ai",
        content: "Oi Felipe! Como está? Esperamos que esteja tudo bem! 😊 Nossa Campanha Junho 2025 está em andamento e estamos precisando muito de doadores O-, que é exatamente seu tipo sanguíneo. Vejo que você é um doador experiente com 5 doações e sempre foi muito comprometido conosco. Sua última doação foi em janeiro, então você já pode doar novamente. Que tal fazer uma doação neste mês? Temos horários bem flexíveis e podemos nos adequar à sua agenda. O tipo O- é universal para doação e é sempre muito necessário! 🩸",
        timestamp: "3 dias 10:15",
        isAI: true,
        campaignId: "camp_1",
      },
      {
        id: "msg_c1_in2_2",
        senderId: "camp1_inativo_2",
        content: "Oi! Obrigado pelo contato. Realmente gostaria de doar, sei que O- é sempre necessário. Mas estou em uma época complicada - vou viajar a trabalho praticamente o mês todo de junho. Vou ficar indo e voltando entre São Paulo e Brasília, então não consigo me comprometer com um agendamento fixo porque nunca sei se vou estar na cidade. Talvez em julho eu consiga me organizar melhor e agendar algo mais certeiro. Vocês podem me lembrar no começo de julho?",
        timestamp: "3 dias 10:20",
        isAI: false,
        campaignId: "camp_1",
      },
      {
        id: "msg_c1_in2_3",
        senderId: "ai",
        content: "Claro, Felipe! Entendemos perfeitamente sua situação com viagens a trabalho. Vou colocar um lembrete para entrarmos em contato no início de julho! 📅 E obrigada por sempre pensar em doar mesmo com a agenda corrida. Isso mostra seu comprometimento com a causa. Boa sorte com as viagens e até julho! ✈️",
        timestamp: "3 dias 10:25",
        isAI: true,
        campaignId: "camp_1",
      }
    ],
    "camp1_inativo_3": [
      {
        id: "msg_c1_in3_1",
        senderId: "ai",
        content: "Olá Patricia! Gostaria de se tornar uma doadora? Posso tirar suas dúvidas!",
        timestamp: "1 sem 09:00",
        isAI: true,
        campaignId: "camp_1",
      },
      {
        id: "msg_c1_in3_2",
        senderId: "camp1_inativo_3",
        content: "Muito obrigada pelas informações. Vou avaliar com meu médico primeiro.",
        timestamp: "1 sem 09:15",
        isAI: false,
        campaignId: "camp_1",
      }
    ]
  },
  camp_2: {
    "camp2_ativo_1": [
      {
        id: "msg_c2_1_1",
        senderId: "ai",
        content: "🚨 URGENTE - Pedro! Esperamos que você esteja bem. Estamos enfrentando uma situação crítica de escassez de sangue tipo O- e precisamos da sua ajuda IMEDIATAMENTE! Temos apenas 1 dia de estoque quando o mínimo seguro são 3 dias. Você é um dos nossos doadores O- mais confiáveis (12 doações!) e sua última doação foi em abril, então você já pode doar. Há pacientes em cirurgias de emergência que dependem do seu tipo sanguíneo AGORA. Você poderia nos ajudar com uma doação nos próximos dias? Esta é realmente uma emergência médica e cada doação O- pode salvar até 4 vidas hoje! 🆘❤️",
        timestamp: "15:15",
        isAI: true,
        campaignId: "camp_2",
      },
      {
        id: "msg_c2_1_2",
        senderId: "camp2_ativo_1",
        content: "Meu Deus! Não sabia que estava tão crítico assim! 😰 É urgência mesmo? Claro que vou ajudar! Posso vir hoje à tarde se for muito necessário, não tenho problema nenhum em mudar minha agenda. Que horário vocês funcionam? Posso ir às 16h ou 17h hoje? E vocês precisam que eu avise mais alguém tipo O- que eu conheço? Tenho alguns amigos que também doam. Conta comigo sempre para emergências assim! Vou sair agora do trabalho se precisar!",
        timestamp: "15:20",
        isAI: false,
        campaignId: "camp_2",
      },
      {
        id: "msg_c2_1_3",
        senderId: "ai",
        content: "Pedro, MUITO OBRIGADA! 🙏 Você é um anjo! Sim, é extremamente urgente - temos pacientes em UTI aguardando. Funcionamos até 18h hoje. 16h30 seria PERFEITO! Vou agendar agora mesmo. E SIM, se você puder avisar seus amigos O-, seria fantástico! Quanto mais pessoas, melhor. Você está literalmente salvando vidas hoje. Te aguardamos às 16h30! Trazer um lanchinho depois? Por nossa conta! Você é incrível! 🌟🩸",
        timestamp: "15:22",
        isAI: true,
        campaignId: "camp_2",
      }
    ],
    "camp2_ativo_2": [
      {
        id: "msg_c2_2_1",
        senderId: "ai",
        content: "Olá! Estamos com necessidade urgente do seu tipo sanguíneo. Você poderia nos ajudar com uma doação nos próximos dias?",
        timestamp: "14:45",
        isAI: true,
        campaignId: "camp_2",
      },
      {
        id: "msg_c2_2_2",
        senderId: "camp2_ativo_2",
        content: "Olá! Sou O- também. Vi a mensagem de urgência. Quando posso ir?",
        timestamp: "14:50",
        isAI: false,
        campaignId: "camp_2",
      }
    ],
    "camp2_aguardando_1": [
      {
        id: "msg_c2_ag1_1",
        senderId: "ai",
        content: "Olá Ana! Precisamos urgentemente do seu tipo sanguíneo. Você pode nos ajudar?",
        timestamp: "13:15",
        isAI: true,
        campaignId: "camp_2",
      },
      {
        id: "msg_c2_ag1_2",
        senderId: "camp2_aguardando_1",
        content: "Claro! Posso ir amanhã de tarde. Que horário têm disponível?",
        timestamp: "13:18",
        isAI: false,
        campaignId: "camp_2",
      },
      {
        id: "msg_c2_ag1_3",
        senderId: "ai",
        content: "Muito obrigada! Agendamento confirmado para amanhã às 14h.",
        timestamp: "13:20",
        isAI: true,
        campaignId: "camp_2",
      }
    ],
    "camp2_inativo_1": [
      {
        id: "msg_c2_in1_1",
        senderId: "ai",
        content: "Carlos, seu tipo sanguíneo está em falta! Você poderia nos ajudar com uma doação?",
        timestamp: "2 dias 11:00",
        isAI: true,
        campaignId: "camp_2",
      },
      {
        id: "msg_c2_in1_2",
        senderId: "camp2_inativo_1",
        content: "Não posso doar agora, estou fazendo tratamento médico.",
        timestamp: "2 dias 11:15",
        isAI: false,
        campaignId: "camp_2",
      }
    ],
    "camp2_inativo_2": [
      {
        id: "msg_c2_in2_1",
        senderId: "ai",
        content: "Oi Beatriz! Estamos precisando urgentemente de O-. Você pode nos ajudar?",
        timestamp: "4 dias 16:00",
        isAI: true,
        campaignId: "camp_2",
      },
      {
        id: "msg_c2_in2_2",
        senderId: "camp2_inativo_2",
        content: "Obrigada pelo contato, mas não tenho disponibilidade esta semana.",
        timestamp: "4 dias 16:10",
        isAI: false,
        campaignId: "camp_2",
      }
    ]
  },
  camp_3: {
    "camp3_ativo_1": [
      {
        id: "msg_c3_1_1",
        senderId: "ai",
        content: "Bom dia, Roberto! Esperamos que você e toda equipe da TechCorp estejam bem! 😊 Estamos entrando em contato porque nossa Campanha de Doadores Corporativos 2025 está em andamento e gostaríamos muito de contar com a participação da TechCorp novamente. Sabemos que vocês sempre demonstraram grande responsabilidade social e suas 20 doações anteriores fizeram uma diferença enorme na vida de muitos pacientes! Nossa campanha oferece um serviço completo: equipe médica vai até a empresa, agendamento flexível, certificado de responsabilidade social e até material para comunicação interna. Sua empresa tem interesse em participar novamente? Podemos organizar tudo de acordo com a agenda de vocês!",
        timestamp: "11:25",
        isAI: true,
        campaignId: "camp_3",
      },
      {
        id: "msg_c3_1_2",
        senderId: "camp3_ativo_1",
        content: "Boa tarde! Que bom receber o contato de vocês! A TechCorp realmente tem muito interesse em participar novamente da campanha. Foi uma experiência excelente da última vez e nossos funcionários ficaram bem motivados. Inclusive, aumentamos nosso quadro de funcionários recentemente e temos mais pessoas interessadas em doar. Queria saber: quantas pessoas vocês conseguem atender por dia quando vão até a empresa? E quais são os requisitos de infraestrutura que precisamos providenciar? Temos um espaço novo que pode ser mais adequado do que o que usamos antes.",
        timestamp: "11:30",
        isAI: false,
        campaignId: "camp_3",
      },
      {
        id: "msg_c3_1_3",
        senderId: "ai",
        content: "Excelente, Roberto! 🌟 Ficamos muito felizes em saber que a experiência foi positiva e que vocês cresceram! Parabéns! Conseguimos atender 15-20 pessoas por dia, dependendo do setup. Para infraestrutura: precisamos de uma sala limpa, bem ventilada, com pelo menos 30m², próxima a banheiro, com macas (levamos equipamentos extras) e ponto de energia. Se tiverem um espaço novo, seria ótimo avaliarmos! Que tal agendarmos uma visita técnica para ver as opções? 📋",
        timestamp: "11:35",
        isAI: true,
        campaignId: "camp_3",
      }
    ],
    "camp3_ativo_2": [
      {
        id: "msg_c3_2_1",
        senderId: "ai",
        content: "Olá Mariana! Que bom ter sua empresa conosco! Podemos organizar uma visita para vocês?",
        timestamp: "10:40",
        isAI: true,
        campaignId: "camp_3",
      },
      {
        id: "msg_c3_2_2",
        senderId: "camp3_ativo_2",
        content: "Perfeito! Nossa equipe de RH já está organizando. Podemos agendar para próxima terça?",
        timestamp: "10:45",
        isAI: false,
        campaignId: "camp_3",
      }
    ],
    "camp3_aguardando_1": [
      {
        id: "msg_c3_ag1_1",
        senderId: "ai",
        content: "Olá Carlos! Sua startup gostaria de participar da nossa campanha corporativa?",
        timestamp: "14:15",
        isAI: true,
        campaignId: "camp_3",
      },
      {
        id: "msg_c3_ag1_2",
        senderId: "camp3_aguardando_1",
        content: "Interessante! Deixe eu ver os detalhes com a equipe.",
        timestamp: "14:18",
        isAI: false,
        campaignId: "camp_3",
      },
      {
        id: "msg_c3_ag1_3",
        senderId: "ai",
        content: "Claro! Enviei o material por email. Aguardo retorno!",
        timestamp: "14:19",
        isAI: true,
        campaignId: "camp_3",
      },
      {
        id: "msg_c3_ag1_4",
        senderId: "camp3_aguardando_1",
        content: "Vou conversar com a diretoria e confirmo na segunda-feira.",
        timestamp: "14:20",
        isAI: false,
        campaignId: "camp_3",
      }
    ],
    "camp3_inativo_1": [
      {
        id: "msg_c3_in1_1",
        senderId: "ai",
        content: "Olá Julia! Sua empresa tem interesse em nossa campanha corporativa de doação?",
        timestamp: "1 sem 15:00",
        isAI: true,
        campaignId: "camp_3",
      },
      {
        id: "msg_c3_in1_2",
        senderId: "camp3_inativo_1",
        content: "Nossa empresa não tem interesse no momento, mas agradecemos o contato.",
        timestamp: "1 sem 15:30",
        isAI: false,
        campaignId: "camp_3",
      }
    ]
  },
  camp_4: {
    "camp4_ativo_1": [
      {
        id: "msg_c4_1_1",
        senderId: "ai",
        content: "Olá Giovana! Muito obrigada pelo seu interesse em se tornar uma doadora de sangue! 🌟 Bem-vinda à nossa Campanha Primeira Doação 2025! Vejo que você tem 26 anos e ainda não doou sangue - que emocionante que essa será sua primeira vez! Nossa campanha foi criada especialmente para pessoas como você, que querem ajudar mas ainda têm dúvidas ou receios. É completamente normal ter perguntas na primeira vez! Temos uma equipe especializada em primeiros doadores, com acompanhamento personalizado do início ao fim. O processo é super seguro, tranquilo e rápido (cerca de 45min). É sua primeira vez mesmo? Posso esclarecer todas suas dúvidas! 😊❤️",
        timestamp: "16:10",
        isAI: true,
        campaignId: "camp_4",
      },
      {
        id: "msg_c4_1_2",
        senderId: "camp4_ativo_1",
        content: "Oi! Muito obrigada pelo acolhimento! 😊 Sim, nunca doei sangue antes e confesso que sempre tive um pouquinho de receio, mas decidi que está na hora de vencer esse medo e fazer algo bom pelos outros. Sempre vejo campanhas e fico pensando que deveria doar, mas nunca tive coragem. Agora resolvi que quero tentar! Vocês podem me explicar direitinho como funciona? Tipo, quanto tempo demora cada etapa? Dói muito? E depois da doação, como eu vou me sentir? Posso dirigir normalmente? Desculpa tantas perguntas, mas quero estar bem preparada! 😅",
        timestamp: "16:15",
        isAI: false,
        campaignId: "camp_4",
      },
      {
        id: "msg_c4_1_3",
        senderId: "ai",
        content: "Giovana, que lindo ver sua coragem! 🥰 Suas perguntas são PERFEITAS e super normais! Vamos por partes: 1) Chegada e cadastro (10min) 2) Entrevista médica - verificamos saúde, peso, pressão (15min) 3) Doação em si (8-12min, bem rapidinho!) 4) Lanchinho e descanso (10min). Sobre dor: só uma picadinha inicial, como exame de sangue. Durante a doação não dói nada! Depois você se sente normal, só evita exercícios pesados no dia. Dirigir pode sim! Nossa equipe te acompanha em cada passo. Que tal agendarmos? 💪✨",
        timestamp: "16:20",
        isAI: true,
        campaignId: "camp_4",
      }
    ],
    "camp4_ativo_2": [
      {
        id: "msg_c4_2_1",
        senderId: "ai",
        content: "Olá Bruno! Que legal você querer começar a doar sangue! É muito simples e seguro. Posso explicar o processo?",
        timestamp: "15:40",
        isAI: true,
        campaignId: "camp_4",
      },
      {
        id: "msg_c4_2_2",
        senderId: "camp4_ativo_2",
        content: "Tenho 18 anos, posso doar? Quais são os requisitos?",
        timestamp: "15:45",
        isAI: false,
        campaignId: "camp_4",
      }
    ],
    "camp4_aguardando_1": [
      {
        id: "msg_c4_ag1_1",
        senderId: "ai",
        content: "Oi Leticia! Parabéns por querer se tornar doadora! Vamos agendar sua primeira doação?",
        timestamp: "12:25",
        isAI: true,
        campaignId: "camp_4",
      },
      {
        id: "msg_c4_ag1_2",
        senderId: "camp4_aguardando_1",
        content: "Sim! Prefiro sábado de manhã se possível.",
        timestamp: "12:28",
        isAI: false,
        campaignId: "camp_4",
      },
      {
        id: "msg_c4_ag1_3",
        senderId: "ai",
        content: "Perfeito! Agendei para sábado às 10h. Muito obrigada!",
        timestamp: "12:30",
        isAI: true,
        campaignId: "camp_4",
      }
    ],
    "camp4_inativo_1": [
      {
        id: "msg_c4_in1_1",
        senderId: "ai",
        content: "Oi Diego! Que tal se tornar um doador de sangue? É um gesto simples que salva vidas!",
        timestamp: "5 dias 14:00",
        isAI: true,
        campaignId: "camp_4",
      },
      {
        id: "msg_c4_in1_2",
        senderId: "camp4_inativo_1",
        content: "Ainda tenho receio. Vou pensar melhor e talvez entre em contato mais tarde.",
        timestamp: "5 dias 14:15",
        isAI: false,
        campaignId: "camp_4",
      }
    ]
  },
  camp_5: {
    "camp5_ativo_1": [
      {
        id: "msg_c5_1_1",
        senderId: "ai",
        content: "Olá Ricardo! Como vai nosso super doador? Já pode agendar sua próxima doação!",
        timestamp: "11:15",
        isAI: true,
        campaignId: "camp_5",
      },
      {
        id: "msg_c5_1_2",
        senderId: "camp5_ativo_1",
        content: "Olá! Gostaria de agendar minha próxima doação. Quando posso ir?",
        timestamp: "11:20",
        isAI: false,
        campaignId: "camp_5",
      }
    ],
    "camp5_aguardando_1": [
      {
        id: "msg_c5_ag1_1",
        senderId: "ai",
        content: "Oi Amanda! Seu período de retorno chegou. Vamos agendar uma nova doação?",
        timestamp: "10:10",
        isAI: true,
        campaignId: "camp_5",
      },
      {
        id: "msg_c5_ag1_2",
        senderId: "camp5_aguardando_1",
        content: "Claro! Mesma hora de sempre está bom para mim.",
        timestamp: "10:12",
        isAI: false,
        campaignId: "camp_5",
      },
      {
        id: "msg_c5_ag1_3",
        senderId: "ai",
        content: "Perfeito! Agendado para quinta às 14h como sempre.",
        timestamp: "10:14",
        isAI: true,
        campaignId: "camp_5",
      },
      {
        id: "msg_c5_ag1_4",
        senderId: "camp5_aguardando_1",
        content: "Obrigada! Vou no horário agendado mesmo.",
        timestamp: "10:15",
        isAI: false,
        campaignId: "camp_5",
      }
    ],
    "camp5_inativo_1": [
      {
        id: "msg_c5_in1_1",
        senderId: "ai",
        content: "Oi Marcos! Que tal agendar sua próxima doação? Você sempre é muito pontual!",
        timestamp: "1 sem 09:00",
        isAI: true,
        campaignId: "camp_5",
      },
      {
        id: "msg_c5_in1_2",
        senderId: "camp5_inativo_1",
        content: "Já doei este mês na campanha anterior. Obrigado pelo lembrete!",
        timestamp: "1 sem 09:10",
        isAI: false,
        campaignId: "camp_5",
      }
    ]
  }
};

// Utilitários para buscar dados por campanha
export const getDonorsByCampaignAndStatus = (campaignId: string, status: ChatStatus): Donor[] => {
  const allDonors = [...mockDonorsCamp1, ...mockDonorsCamp2, ...mockDonorsCamp3, ...mockDonorsCamp4, ...mockDonorsCamp5];
  
  return allDonors.filter(donor => {
    if (donor.campaignId !== campaignId) return false;
    
    switch (status) {
      case 'ativos':
        return donor.unread > 0;
      case 'aguardando':
        return donor.unread === 0 && donor.hasActiveConversation;
      case 'inativo':
        return !donor.hasActiveConversation;
      default:
        return true;
    }
  });
};

export const getMessagesByCampaign = (donorId: string, campaignId: string): Message[] => {
  return messagesByCampaign[campaignId]?.[donorId] || [];
};

export const getCampaignById = (campaignId: string): Campaign | undefined => {
  return mockCampaigns.find(campaign => campaign.id === campaignId);
};

export const getActiveCampaigns = (): Campaign[] => {
  return mockCampaigns.filter(campaign => campaign.status === 'active');
};

export const getAllDonorsByStatus = (status: ChatStatus): Donor[] => {
  const allDonors = [...mockDonorsCamp1, ...mockDonorsCamp2, ...mockDonorsCamp3, ...mockDonorsCamp4, ...mockDonorsCamp5];
  
  return allDonors.filter(donor => {
    switch (status) {
      case 'ativos':
        return donor.unread > 0;
      case 'aguardando':
        return donor.unread === 0 && donor.hasActiveConversation;
      case 'inativo':
        return !donor.hasActiveConversation;
      default:
        return true;
    }
  });
};