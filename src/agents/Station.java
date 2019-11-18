package agents;

import static jade.lang.acl.MessageTemplate.MatchOntology;
import static jade.lang.acl.MessageTemplate.MatchPerformative;
import static jade.lang.acl.MessageTemplate.and;
import static jade.lang.acl.MessageTemplate.or;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREInitiator;
import simulation.World;
import types.ClientRepairs;
import types.JobList;
import types.Proposal;
import types.Repair;
import types.RepairKey;
import types.RepairList;
import utils.Logger;
import utils.MalfunctionType;

public class Station extends Agent {
    private static final long serialVersionUID = 3322670743911601747L;

    private final String id;
    private final Set<AID> clients;
    private final Set<AID> companies;
    private HashMap<AID, HashMap<Integer, Repair>> repairsQueue;

    public Station(String id) {
        assert id != null;
        this.id = id;
        this.clients = new HashSet<>();
        this.companies = new HashSet<>();
        this.repairsQueue = new HashMap<>();

        // TODO LOGIC: replace String with a proper data structure for state tracking
    }

    @Override
    protected void setup() {
        Logger.info(getLocalName(), "Setup " + id);

        registerDFService();

        String clientSub = World.get().getClientStationService();
        String companySub = World.get().getCompanyStationService();
        addBehaviour(new SubscriptionListener(this, clientSub, clients));
        addBehaviour(new SubscriptionListener(this, companySub, companies));

        // TODO COMMS: in a loop, in this order (during the night)
        addBehaviour(new FetchNewMalfunctions(this));
        addBehaviour(new AssignJobs(this));
    }

    @Override
    protected void takeDown() {
        Logger.warn(getLocalName(), "Station Terminated!");
    }

    private void registerDFService() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setName("station-" + id);
        sd.setType(World.get().getStationType());

        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    private ACLMessage prepareClientPromptMessage() {
        ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
        message.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        message.setOntology(World.get().getPromptClient());
        for (AID client : clients) message.addReceiver(client);
        return message;
    }

    private ACLMessage prepareCompanyQueryMessage() {
        JobList jobList = prepareJobList();
        ACLMessage message = new ACLMessage(ACLMessage.REQUEST);
        message.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
        message.setOntology(World.get().getInformCompanyJobs());
        message.setContent(jobList.make());
        for (AID company : companies) message.addReceiver(company);
        return message;
    }

    private JobList prepareJobList() {
        easy = groupRepairs(MalfunctionType.EASY);
        medium = groupRepairs(MalfunctionType.MEDIUM);
        hard = groupRepairs(MalfunctionType.HARD);
        return new JobList(easy.length, medium.length, hard.length);
    }

    // ***** UTILITIES

    class Assignment {
        final Map<AID, Proposal> proposals;
        final Map<AID, Proposal> assignments;
        final Map<AID, RepairList> repairs;

        private RepairKey[] get(MalfunctionType type) {
            switch (type) {
            case EASY:
                return easy;
            case MEDIUM:
                return medium;
            case HARD:
                return hard;
            }
            return null;
        }

        private void order(MalfunctionType type) {
            ArrayList<AID> list = new ArrayList<>();
            for (Proposal proposal : proposals.values()) {
                if (proposal.get(type) > 0) list.add(proposal.company);
            }
            Collections.sort(list, new Comparator<AID>() {
                @Override
                public int compare(AID lhs, AID rhs) {
                    double l = proposals.get(lhs).getPrice(type);
                    double r = proposals.get(rhs).getPrice(type);
                    return l < r ? -1 : l > r ? 1 : 0;
                }
            });

            AID[] companies = (AID[]) list.toArray();
            RepairKey[] keys = get(type);

            int c = 0, r = 0;
            while (c < companies.length && r < keys.length) {
                double price = proposals.get(companies[c]).getPrice(type);
                double max = repairsQueue.get(keys[r].client).get(keys[r].id).getPrice();
                if (price <= max) {
                    assignments.get(companies[c]).add(type, 1);
                    repairs.get(keys[r].client).ids.add(keys[r].id);
                } else {
                    break;
                }
            }
        }

        Assignment(Map<AID, Proposal> proposals) {
            this.proposals = proposals;
            this.assignments = new HashMap<>();
            this.repairs = new HashMap<>();

            for (AID company : proposals.keySet()) assignments.put(company, new Proposal(company));
            for (AID client : clients) repairs.put(client, new RepairList());

            order(MalfunctionType.EASY);
            order(MalfunctionType.MEDIUM);
            order(MalfunctionType.HARD);
        }
    }

    private RepairKey[] easy, medium, hard;

    private RepairKey[] groupRepairs(MalfunctionType type) {
        ArrayList<RepairKey> list = new ArrayList<>();

        for (AID client : repairsQueue.keySet()) {
            HashMap<Integer, Repair> repairs = repairsQueue.get(client);
            for (Integer id : repairs.keySet()) {
                Repair repair = repairs.get(id);
                if (type.equals(repair.getMalfunctionType())) {
                    list.add(new RepairKey(client, id));
                }
            }
        }

        RepairKey[] array = (RepairKey[]) list.toArray();
        Arrays.sort(array, new Comparator<RepairKey>() {
            @Override
            public int compare(RepairKey lhs, RepairKey rhs) {
                double l = repairsQueue.get(lhs.client).get(lhs.id).getPrice();
                double r = repairsQueue.get(rhs.client).get(rhs.id).getPrice();
                return l > r ? -1 : l < r ? 1 : 0;
            }
        });

        return array;
    }

    // ***** BEHAVIOURS

    private class FetchNewMalfunctions extends AchieveREInitiator {
        private static final long serialVersionUID = 8662470226125479639L;

        public FetchNewMalfunctions(Agent a) {
            super(a, prepareClientPromptMessage());  // Protocol A
        }

        @Override  // Protocol B
        protected void handleInform(ACLMessage inform) {
            AID client = inform.getSender();
            assert clients.contains(client);
            ClientRepairs repairs = ClientRepairs.from(inform);

            if (!repairsQueue.containsKey(client)) repairsQueue.put(client, new HashMap<>());

            for (int id : repairs.list.keySet()) {
                repairsQueue.get(client).put(id, repairs.list.get(id));
            }

            // TODO COMMS: verify this does indeed finish when all clients respond.
        }
    }

    private class AssignJobs extends AchieveREInitiator {
        private static final long serialVersionUID = -6775360046825661442L;

        AssignJobs(Agent a) {
            super(a, prepareCompanyQueryMessage());  // Protocol C
        }

        private void informCompany(AID company, Proposal content) {
            ACLMessage message = new ACLMessage(ACLMessage.INFORM);
            message.setOntology(World.get().getInformCompanyAssignment());
            message.addReceiver(company);
            message.setContent(content.make());
            send(message);  // Protocol E
        }

        private void informClient(AID client, RepairList content) {
            ACLMessage message = new ACLMessage(ACLMessage.INFORM);
            message.setOntology(World.get().getInformClient());
            message.addReceiver(client);
            message.setContent(content.make());
            send(message);  // Protocol F
        }

        @Override  // Protocol D
        protected void handleAllResultNotifications(Vector resultNotifications) {
            int N = resultNotifications.size();
            ACLMessage[] messages = new ACLMessage[N];
            resultNotifications.copyInto(messages);

            Map<AID, Proposal> proposals = new HashMap<>();
            for (int i = 0; i < N; ++i) {
                AID company = messages[i].getSender();
                proposals.put(company, Proposal.from(company, messages[i]));
            }

            Assignment assignment = new Assignment(proposals);

            for (AID company : assignment.assignments.keySet()) {
                Proposal proposal = assignment.assignments.get(company);
                informCompany(company, proposal);
            }

            for (AID client : assignment.repairs.keySet()) {
                RepairList list = assignment.repairs.get(client);
                informClient(client, list);
                for (int id : list.ids) repairsQueue.get(client).remove(id);
            }
        }
    }

    private class SubscriptionListener extends CyclicBehaviour {
        private static final long serialVersionUID = 9068977292715279066L;

        private final MessageTemplate mt;
        private final Set<AID> subscribers;

        SubscriptionListener(Agent a, String ontology, Set<AID> subscribers) {
            super(a);
            this.subscribers = subscribers;

            MessageTemplate subscribe = MatchPerformative(ACLMessage.SUBSCRIBE);
            MessageTemplate unsubscribe = MatchPerformative(ACLMessage.CANCEL);
            MessageTemplate onto = MatchOntology(ontology);
            this.mt = and(or(subscribe, unsubscribe), onto);
        }

        @Override
        public void action() {
            ACLMessage message = receive(mt);
            while (message == null) {
                block();
                return;
            }

            if (message.getPerformative() == ACLMessage.SUBSCRIBE) {
                this.subscribers.add(message.getSender());
            } else /* ACLMessage.CANCEL */ {
                this.subscribers.remove(message.getSender());
            }

            message.createReply();
            message.setPerformative(ACLMessage.CONFIRM);
            send(message);
        }
    }
}
