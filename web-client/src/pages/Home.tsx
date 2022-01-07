import {useEffect, useReducer, useRef} from 'react';
import {Button, Grid, Card, CardContent, Typography, CircularProgress} from '@mui/material';
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
};

type AppState = {
    disasters: Disaster[],
    hotPoints: [Location, number][],
};

type Action = { type: 'CLEAR' } | {
    type: 'ADD_DISASTER',
    disaster: Disaster,
} | {
    type: 'SET_HOT_POINTS',
    hotPoints: [Location, number][],
};

const initialState: AppState = {
    disasters: [],
    hotPoints: [],
};

export const HomePage = () => {
    const ws = useRef<WebSocket | null>(null);
    const [state, dispatch] = useReducer(reducer, initialState);

    const connectToGoogleCalendar = async () => {
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
                <Button onClick={connectToGoogleCalendar} variant='contained'>Connect to Google Calendar</Button>
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
        default:
            return state;
    }
};