import {useEffect, useReducer, useRef} from 'react';
import {
    Button,
    Grid,
    Card,
    CardContent,
    Typography,
    CircularProgress,
    TableContainer,
    Paper,
    TableCell, Table, TableRow, TableHead, TableBody
} from '@mui/material';
import moment from 'moment';
import {getGoogleToken} from '../utils';

type Disaster = {
    id: string,
    title: string,
    location: Location[],
    startDate: string | undefined,
    endDate: string | undefined,
};

type Location = {
    latitude: number,
    longitude: number,
};

type CalendarEvent = {
    event: string,
    start: string,
    safetyLevel: SafetyLevel,
}

type SafetyLevel = 'Ok' | 'WithinDisaster' | 'WithinHotPoint';

type WebSocketMessage = {
    action: 'set_calendar_token',
    token: string,
};

type IncomingWebSocketMessage = {
    action: 'new_disaster',
    data: Disaster,
} | {
    action: 'hot_points',
    data: [Location, number][],
} | {
    action: 'calendar_event',
    data: CalendarEvent,
};

type AppState = {
    disasters: Disaster[],
    hotPoints: [Location, number][],
    events: CalendarEvent[],
    calendarConnected: boolean,
};

type Action = { type: 'CLEAR' } | {
    type: 'ADD_DISASTER',
    disaster: Disaster,
} | {
    type: 'SET_HOT_POINTS',
    hotPoints: [Location, number][],
} | {
    type: 'CALENDAR_CONNECTED',
} | {
    type: 'ADD_CALENDAR_EVENT',
    event: CalendarEvent,
};

const initialState: AppState = {
    disasters: [],
    hotPoints: [],
    events: [],
    calendarConnected: false,
};

export const HomePage = () => {
    const ws = useRef<WebSocket | null>(null);
    const [state, dispatch] = useReducer(reducer, initialState);

    const connectToGoogleCalendar = async () => {
        dispatch({ type: 'CALENDAR_CONNECTED' });
        const token = await getGoogleToken();

        if (ws.current !== null) {
            ws.current.send(JSON.stringify({ action: 'set_calendar_token', token } as WebSocketMessage));
        }
    };

    useEffect(() => {
        dispatch({ type: 'CLEAR' });

        ws.current = new WebSocket('ws://localhost:8080/ws');
        ws.current.onmessage = msg => {
            const message = JSON.parse(msg.data) as IncomingWebSocketMessage;
            if (message.action === 'new_disaster') {
                dispatch({ type: 'ADD_DISASTER', disaster: message.data });
            } else if (message.action === 'hot_points') {
                dispatch({ type: 'SET_HOT_POINTS', hotPoints: message.data });
            } else if (message.action === 'calendar_event') {
                dispatch({ type: 'ADD_CALENDAR_EVENT', event: message.data });
            }
        };

        const wsCurrent = ws.current;
        return () => wsCurrent.close();
    }, []);

    const hotPointsCards = (
        <Grid container spacing={1}>
            { state.hotPoints.map(hotPoint => (
                <Grid item xs={3}>
                    <HotPointCard hotPoint={hotPoint} />
                </Grid>
            )) }
        </Grid>
    );

    const eventsTable = state.calendarConnected === false ? (
        <Button onClick={connectToGoogleCalendar} variant='contained'>Connect to Google Calendar</Button>
    ) : (state.events.length === 0 ? (<CircularProgress />) : (<CalendarEventsTable events={state.events} />));

    return (
        <Grid container spacing={2}>
            <Grid item xs={3}>
                <Typography variant='h1' sx={{fontSize: 26}} style={{padding: '8px 0'}}>Disaster Feed</Typography>
                { state.disasters.sort((a, b) => {
                    const aDate = a.startDate !== undefined ? Date.parse(a.startDate) : Date.now();
                    const bDate = b.startDate !== undefined ? Date.parse(b.startDate) : Date.now();
                    return bDate - aDate;
                }).map(disaster => <DisasterCard key={disaster.id} disaster={disaster} />) }
            </Grid>
            <Grid item xs={9}>
                <Typography variant='h1' sx={{fontSize: 26}} style={{padding: '8px 0'}}>Hot Points</Typography>
                { state.hotPoints.length === 0 ? (<CircularProgress />) : hotPointsCards }

                <Typography variant='h1' sx={{fontSize: 26}} style={{padding: '8px 0'}}>Your events</Typography>
                { eventsTable }
            </Grid>
        </Grid>
    );
};

const DisasterCard = (props: { disaster: Disaster }) => {
    return (
        <Card style={{ marginBottom: '8px' }}>
            <CardContent>
                <Typography variant='h5' component='div' sx={{ fontSize: 18 }}>
                    { props.disaster.title }
                </Typography>
                <Typography variant='h5' component='div' sx={{ fontSize: 12 }}>
                    { props.disaster.location.map(location => location.latitude + ", " + location.longitude).join("; ") }
                </Typography>
                <Typography variant='h5' component='div' sx={{ fontSize: 14 }}>
                    { props.disaster.startDate === undefined ? 'Currently active' : moment(props.disaster.startDate).fromNow() }
                </Typography>
            </CardContent>
        </Card>
    );
}

const HotPointCard = (props: { hotPoint: [Location, number] }) => {
    return (
        <Card style={{ marginBottom: '8px' }}>
            <CardContent>
                <Typography variant='h5' component='div' sx={{ fontSize: 18 }}>
                    { props.hotPoint[0].latitude }, { props.hotPoint[0].longitude }
                </Typography>
                <Typography variant='h5' component='div' sx={{ fontSize: 14 }}>
                    { props.hotPoint[1] } disasters observed here
                </Typography>
            </CardContent>
        </Card>
    );
}

const CalendarEventsTable = (props: { events: CalendarEvent[] }) => {
    const sortedEvents = props.events.sort((a, b) => {
        const aDate = Date.parse(a.start);
        const bDate = Date.parse(b.start);
        return aDate - bDate;
    });

    return (
        <TableContainer component={Paper}>
            <Table>
                <TableHead>
                    <TableRow>
                        <TableCell>Event</TableCell>
                        <TableCell>Time remaining</TableCell>
                        <TableCell>Safety level</TableCell>
                    </TableRow>
                </TableHead>
                <TableBody>
                    { sortedEvents.map(event => (
                        <TableRow>
                            <TableCell>{event.event}</TableCell>
                            <TableCell>{moment(event.start).fromNow()}</TableCell>
                            <TableCell style={{ color: safetyLevelToColor(event.safetyLevel) }}>{ safetyLevelToText(event.safetyLevel) }</TableCell>
                        </TableRow>
                    )) }
                </TableBody>
            </Table>
        </TableContainer>
    );
};

const safetyLevelToColor = (safetyLevel: SafetyLevel): string => {
    switch (safetyLevel) {
        case 'Ok':
            return 'green';
        case 'WithinDisaster':
            return 'red';
        case 'WithinHotPoint':
            return 'yellow';
    }
}

const safetyLevelToText = (safetyLevel: SafetyLevel): string => {
    switch (safetyLevel) {
        case 'Ok':
            return 'ok';
        case 'WithinDisaster':
            return 'within disaster';
        case 'WithinHotPoint':
            return 'within hot point';
    }
};

const reducer = (state: AppState, action: Action) => {
    switch (action.type) {
        case 'CLEAR':
            return initialState;
        case 'ADD_DISASTER':
            return {
                ...state,
                disasters: [
                    action.disaster,
                    ...state.disasters
                ],
            };
        case 'SET_HOT_POINTS':
            return {
                ...state,
                hotPoints: action.hotPoints,
            };
        case 'CALENDAR_CONNECTED':
            return {
                ...state,
                calendarConnected: true,
            };
        case 'ADD_CALENDAR_EVENT':
            return {
                ...state,
                events: [
                    ...state.events,
                    action.event,
                ]
            };
        default:
            return state;
    }
};